package com.olivertechnology.chargeoff.model;

import com.olivertechnology.chargeoff.util.SQSUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.UUID;

@Component
@Data
@Slf4j
public class ChargeoffSqs {

  public String requestID;
  public String chargeOffDate;
  public String chargeOffAmount;
  public String matterUuid;
  public  String accountNumber;

  static String queueEndPoint;
  static String queueName;

  public static String chargeoffSqs(String rRequestID, String rMatterUuid, String rChargeOffDate,
                                    String rChargeOffAmount, String rAccountNumber) {
    log.info("chargeoffSqs");
    log.info("rRequestID=" + rRequestID);
    log.info("rMatterUuid=" + rMatterUuid);
    log.info("rChargeOffDate=" + rChargeOffDate);
    log.info("rChargeOffAmount=" + rChargeOffAmount);
    log.info("rAccountNumber=" + rAccountNumber);

    String result = "success";

    Timestamp ts = new Timestamp(System.currentTimeMillis());
    SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    String timestamp = sdf1.format(ts);
    SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.722+SSS");
    String eventDate = sdf2.format(ts);

    String bRequestID = UUID.randomUUID().toString();

	// Create SQS message
    String sqsMessage =
      "{ "
        + "\"envelope\": { "
          + "\"httpMethod\": \"POST\", "
          + "\"customer\": \"wells\", "
          + "\"eventType\": \"CHARGE_OFF\", "
          + "\"requestID\": \"" + rRequestID + "\", "
          + "\"timestamp\": \"" + timestamp + "\" "
    + "}, "
    + "\"requestBody\": { "
        + "\"requestID\": \"" + bRequestID + "\", "
        + "\"accountNumber\": \"" + rAccountNumber + "\", "
        + "\"eventName\": \"CHARGE_OFF\", "
        + "\"eventDate\": \"" + eventDate + "\", "
        + "\"payload\": { "
          + "\"matterID\": \"" + rMatterUuid + "\", "
          + "\"chargeOffDate\": \"" + rChargeOffDate + "\", "
          + "\"chargeOffAmount\": \"" + rChargeOffAmount + "\" "
        + "} "
      + "} "
    + "}";
    log.info("sqsMessage=" + sqsMessage);

    log.info("queueEndPoint=" + queueEndPoint);
    log.info("queueName=" + queueName);

    SQSUtils sqs = new SQSUtils();
    String retrieval = sqs.pushToDelayQueue(queueEndPoint + queueName, sqsMessage);
    log.info("retrieval=" + retrieval);
    if (retrieval.contains("failure")) {
      result = "sqs failure";
    }
    
    // Return status.
    return "{ result: \"" + result + "\" }";
  }

  @Value("${SQS.queueEndPoint}")
  public void setQueueEndPoint(String queueEndPoint) {
    ChargeoffSqs.queueEndPoint = queueEndPoint;
  }

  @Value("${SQS.queueName}")
  public void setQueueName(String queueName) {
    ChargeoffSqs.queueName = queueName;
  }
}

