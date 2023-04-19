package com.olivertechnology.chargeoff.model;

import com.olivertechnology.chargeoff.util.DBUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.UUID;
@Component
@Data
@Slf4j
public class ChargeoffReversal {

  public String chargeOffDate;
  public String chargeOffAmount;
  public String matterUuid;
  public String requestID;
  public String accountNumber;
  static String noaLetterTemplate;

  static String mailDocumentUrl;

  public static String chargeoffReversal(String rMatterUuid, String rAccountNumber) {
    log.info("chargeoffReversal");
    log.info("rMatterUuid=" + rMatterUuid);
    log.info("rAccountNumber=" + rAccountNumber);

    if (rMatterUuid == null || rMatterUuid.isBlank() || rMatterUuid.isEmpty()) {
      return "no matter uuid failure";
    }
    if (rAccountNumber == null || rAccountNumber.isBlank() || rAccountNumber.isEmpty()) {
      return "no account number failure";
    }

    String result = "success";

    // Get database connection
    DBUtils db = new DBUtils();

    String chargeoffRes = db.doChargeoff(null, null, rMatterUuid, rAccountNumber);
    log.info("chargeoffRes=" + chargeoffRes);
    if (chargeoffRes.contains("failure")) {
      result = "chargeoff failure";
    }

    // Add an audit entry for this.
    //FIXME -- need details on what needs to go here.

    String rRequestID = UUID.randomUUID().toString();

    // Get the correct event configuration id
    String eventConfigurationUuid = db.getEventConfigurationUuid("CHARGE_OFF_REVERSAL");
    log.info("eventConfigurationUuid=" + eventConfigurationUuid);
    if (eventConfigurationUuid != null && !eventConfigurationUuid.isBlank() && !eventConfigurationUuid.isEmpty()) {
      // Create an event FIXME -- is this correct?
      Timestamp ts = new Timestamp(System.currentTimeMillis());
      SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
      String currentDate = sdf1.format(ts);
      String insertEventRes = db.insertEvent(rMatterUuid, rRequestID, currentDate, eventConfigurationUuid);
      if (insertEventRes.contains("failute")) {
        result = "insertEvent failure";
      }
    } else {
      result = "eventConfigurationUuid failure";
    }

    log.info("Processing complete");

    return "{ result: \"" + result + "\" }";
  }
}

