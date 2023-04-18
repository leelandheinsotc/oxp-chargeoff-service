package com.olivertechnology.chargeoff.util;

import java.sql.*;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DBUtils {

  static String matterSecretName;
  static String oxpDbSecretName;
  static String adtDbSecretName;
  static String secretRegion;
  @Autowired
  Environment env;

  @Value("${db.matterSecret}")
  public void setMatterSecret(String matterSecretName) {
    DBUtils.matterSecretName = matterSecretName;
  }

  @Value("${db.oxpDbSecret}")
  public void setOxpDbSecret(String oxpDbSecretName) {
    DBUtils.oxpDbSecretName = oxpDbSecretName;
  }

  @Value("${db.adtDbSecret}")
  public void setAdtDbSecret(String adtDbSecretName) {
    DBUtils.adtDbSecretName = adtDbSecretName;
  }

  @Value("${db.dbRegion}")
  public void setRegion(String secretRegion) {
    DBUtils.secretRegion = secretRegion;
  }

  public String doChargeoff(String chargeOffDate, String chargeOffAmount, String matterUuid, String accountNumber) {
    log.info("Doing Chargeoff");

    log.info("chargeOffDate=" + chargeOffDate);
    log.info("chargeOffAmount=" + chargeOffAmount);
    log.info("matterUuid=" + matterUuid);
    log.info("accountNumber=" + accountNumber);

    Connection c = null;
    Statement stmt = null;

    log.info("matterSecretName=" + matterSecretName);
    log.info("secretRegion=" + secretRegion);
    DBDetails details = getDBDetails(matterSecretName);

    try {
      Class.forName("org.postgresql.Driver");

      c = DriverManager.getConnection(details.getUrl(), details.getUsername(),
              details.getPassword());

      log.debug("Successfully Connected to operations db to do chargeoff.");
      stmt = c.createStatement();

      // Update charge_off_date in accounts table.
      String updateAccountsSql = "UPDATE accounts SET last_updated = now(), "
              + "charge_off_date = '" + chargeOffDate + "', "
              + "charge_off_amount = " + chargeOffAmount + " "
              + "WHERE matter_uuid = '" + matterUuid + "' AND account_number = '" + accountNumber + "'";
      log.info("updateAccountsSql=" + updateAccountsSql);
      stmt.executeUpdate(updateAccountsSql);

      // Update last updated date on matter
      String updateMattersSql = "UPDATE matters SET last_updated = now() WHERE matter_uuid = '" + matterUuid + "'";
      log.info("updateMattersSql=" + updateMattersSql);
      stmt.executeUpdate(updateMattersSql);
    } catch (Exception e) {
      log.error("chargeoff error: ", e);
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        }
        if (c != null) {
          c.close();
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    return "Success";
  }

  public String getEventUuid(String matterUuid) {
    log.info("getEventUuid");
    log.info("matterUuid=" + matterUuid);

    String eventUuid = null;

    Connection c = null;
    Statement stmt = null;

    //System.out.println("matterSecretName=" + matterSecretName);
    //System.out.println("secretRegion=" + secretRegion);
    DBDetails details = getDBDetails(matterSecretName);

    ResultSet rsdm = null;

    try {
      Class.forName("org.postgresql.Driver");

      c = DriverManager.getConnection(details.getUrl(), details.getUsername(),
              details.getPassword());

      log.debug("Successfully Connected to operations db to get event uuid.");
      stmt = c.createStatement();

      //FIXME -- waiting for real details on how to query events table
      String eventSql = "SELECT e.event_uuid FROM events e, event_configurations ec"
              + " WHERE e.matter_uuid = '" + matterUuid + "'"
              + " AND e.event_configuration_uuid = ec.event_configuration_uuid"
              + " AND ec.event_name = 'CHARGE_OFF'";
      log.info("eventSql=" + eventSql);
      rsdm = stmt.executeQuery(eventSql);
      while (rsdm.next()) {
        eventUuid = rsdm.getString("event_uuid");
      }
      log.info("eventUuid=" + eventUuid);
    } catch (Exception e) {
      log.error("get event uuid error: {}", e);
      eventUuid = "failure";
    } finally {
      try {
        if (rsdm != null) {
          rsdm.close();
        }
        if (stmt != null) {
          stmt.close();
        }
        if (c != null) {
          c.close();
        }
      } catch (SQLException e) {
        log.error("getDataFields SQL error: {}", e);
        throw new RuntimeException(e);
      }
    }

    return eventUuid;
  }

  public String getEventConfigurationUuid(String event) {
    log.info("getEventConfigurationUuid");

    String eventConfigurationUuid = null;

    Connection c = null;
    Statement stmt = null;

    //System.out.println("matterSecretName=" + matterSecretName);
    //System.out.println("secretRegion=" + secretRegion);
    DBDetails details = getDBDetails(matterSecretName);

    ResultSet rsdm = null;

    try {
      Class.forName("org.postgresql.Driver");

      c = DriverManager.getConnection(details.getUrl(), details.getUsername(),
              details.getPassword());

      log.debug("Successfully Connected to operations db to get event configuration uuid.");
      stmt = c.createStatement();

      String ecSql = "SELECT event_configuration_uuid FROM event_configurations WHERE event_name = '" + event + "' AND is_active = true";
      log.info("ecSql=" + ecSql);
      rsdm = stmt.executeQuery(ecSql);
      while (rsdm.next()) {
        eventConfigurationUuid = rsdm.getString("event_configuration_uuid");
      }
      log.info("eventConfigurationUuid=" + eventConfigurationUuid);
    } catch (Exception e) {
      log.error("get event configuration uuid error: {}", e);
      eventConfigurationUuid = "failure";
    } finally {
      try {
        if (rsdm != null) {
          rsdm.close();
        }
        if (stmt != null) {
          stmt.close();
        }
        if (c != null) {
          c.close();
        }
      } catch (SQLException e) {
        log.error("getDataFields SQL error: {}", e);
        throw new RuntimeException(e);
      }
    }

    return eventConfigurationUuid;
  }

  public String insertEvent(String matterUuid, String requestID, String chargeOffDate, String eventConfigurationUuid) {
    log.info("insertEvent");
    log.info("matterUuid=" + matterUuid);
    log.info("requestID=" + requestID);
    log.info("chargeOffDate=" + chargeOffDate);
    log.info("eventConfigurationUuid=" + eventConfigurationUuid);

    String result = "success";

    Connection c = null;
    Statement stmt = null;

    log.info("matterSecretName=" + matterSecretName);
    log.info("secretRegion=" + secretRegion);
    DBDetails details = getDBDetails(matterSecretName);

    try {
      Class.forName("org.postgresql.Driver");

      c = DriverManager.getConnection(details.getUrl(), details.getUsername(),
              details.getPassword());

      log.debug("Successfully Connected to operations db to insert event.");
      stmt = c.createStatement();
      String insertEventsSql = "INSERT INTO events (matter_uuid, request_id, event_details_description, "
          + "occured_on, created_on, event_configuration_uuid, event_uuid) "
          + "VALUES ('" + matterUuid + "', '" + requestID + "', '" + "', '" + chargeOffDate + "', now(), '" + eventConfigurationUuid + "', "
          + "pg_catalog.gen_random_uuid())";
      log.info("insertEventsSql=" + insertEventsSql);
      stmt.executeUpdate(insertEventsSql);
    } catch (Exception e) {
      log.error("insertEvent error: " + e);
      result = "failure";
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        }
        if (c != null) {
          c.close();
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    return result;
  }

  public DBDetails getDBDetails(String secretName) {
    DBDetails details = new DBDetails();

    //log.info("getDBDetails:secretRegion=" + secretRegion);
    //log.info("getDBDetails:secretName=" + secretName);

    JSONObject secretObj = SecretManager.getSecret(secretName, secretRegion);
    String username = secretObj.getAsString("username");
    String password = secretObj.getAsString("password");
    String host = secretObj.getAsString("host");
    String dbname = secretObj.getAsString("dbname");

    log.info("username=" + username);
    log.info("password=" + password);
    log.info("host=" + host);
    log.info("dbname=" + dbname);

    String url = "jdbc:postgresql://" + host + "/" + dbname;

    details.setUrl(url);
    details.setUsername(username);
    details.setPassword(password);

    return details;
  }
}



