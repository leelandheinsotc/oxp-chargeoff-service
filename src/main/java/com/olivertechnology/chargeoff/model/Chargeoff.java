package com.olivertechnology.chargeoff.model;

import com.olivertechnology.chargeoff.util.DBUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.UUID;

@Component
@Data
@Slf4j
public class Chargeoff {

  public String chargeOffDate;
  public String chargeOffAmount;
  public String matterUuid;
  public String requestID;
  public String accountNumber;
  static String noaLetterTemplate;

  static String mailDocumentUrl;

  @Value("${dm.noaTemplate}")
  public void setNoaLetterTemplate(String noaLetterTemplate) { Chargeoff.noaLetterTemplate = noaLetterTemplate; }

  @Value("${dm.mailDocumentUrl}")
  public void setMailDocumentUrl(String mailDocumentUrl) { Chargeoff.mailDocumentUrl = mailDocumentUrl; }


  public static String chargeoff(String rMatterUuid, String rChargeOffDate,
                                 String rChargeOffAmount, String rAccountNumber) {
    log.info("chargeoff");
    log.info("rMatterUuid=" + rMatterUuid);
    log.info("rChargeOffDate=" + rChargeOffDate);
    log.info("rChargeOffAmount=" + rChargeOffAmount);
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

    String chargeoffRes = db.doChargeoff(rChargeOffDate, rChargeOffAmount, rMatterUuid, rAccountNumber);
    log.info("chargeoffRes=" + chargeoffRes);
    if (chargeoffRes.contains("failure")) {
      result = "chargeoff failure";
    }

    String eventUuid = db.getEventUuid(rMatterUuid);
    log.info("eventUuid=" + eventUuid);
    if (eventUuid != null && eventUuid.contains("failure")) {
      result = "db failure";
    }

    if (eventUuid != null && !eventUuid.isBlank() && !eventUuid.isEmpty()) {
      log.info("Letter already sent");
    } else {
      log.info("calling letterservice to send mail");
      String jsonBody = "{ \"matterUuid\" : \"" + rMatterUuid + "\", \"templateReference\" : \"" + noaLetterTemplate + "\" }";
      log.info("jsonBody=" + jsonBody);

      log.info("posting to " + mailDocumentUrl);

      // Call document management service to send the NOA letter
      CloseableHttpClient httpClient = HttpClients.createDefault();
      try {
        HttpEntity postData = EntityBuilder.create()
                .setContentType(ContentType.parse("application/json"))
                .setText(jsonBody)
                .build();
        HttpUriRequest req = RequestBuilder
                .post(mailDocumentUrl)
                .setEntity(postData)
                .build();
        CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(req);
        log.info("response=" + response);
      } catch (Exception ex) {
        log.error("Letter Service failure - " + ex);
        result = "Letter Service failure";
      } finally {
        try {
          httpClient.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      // Send notification to 1CCC
      //FIXME -- per Robert we don't have details on how to do this yet.
    }

    // Add an audit entry for this.
    //FIXME -- need details on what needs to go here.

    String rRequestID = UUID.randomUUID().toString();

    // Get the correct event configuration id
    String eventConfigurationUuid = db.getEventConfigurationUuid("CHARGE_OFF");
    log.info("eventConfigurationUuid=" + eventConfigurationUuid);
    if (eventConfigurationUuid != null && !eventConfigurationUuid.isBlank() && !eventConfigurationUuid.isEmpty()) {
        // Create an event FIXME -- is this correct?
        String insertEventRes = db.insertEvent(rMatterUuid, rRequestID, rChargeOffDate, eventConfigurationUuid);
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

