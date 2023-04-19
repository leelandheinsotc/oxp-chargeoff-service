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
public class ChargeoffReversalSqs {

  public String requestID;
  public String chargeOffDate;
  public String chargeOffAmount;
  public String matterUuid;
  public  String accountNumber;

  static String queueEndPoint;
  static String inboundQueueName;

  public static String chargeoffReversalSqs(String rRequestID, String rMatterUuid, String rAccountNumber) {
    log.info("chargeoffReversalSqs");
    log.info("rRequestID=" + rRequestID);
    log.info("rMatterUuid=" + rMatterUuid);
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
          + "\"eventType\": \"CHARGE_OFF_REVERSAL\", "
          + "\"requestID\": \"" + rRequestID + "\", "
          + "\"timestamp\": \"" + timestamp + "\" "
    + "}, "
    + "\"requestBody\": { "
        + "\"requestID\": \"" + bRequestID + "\", "
        + "\"accountNumber\": \"" + rAccountNumber + "\", "
        + "\"eventName\": \"CHARGE_OFF\", "
        + "\"eventDate\": \"" + eventDate + "\", "
        + "\"payload\": { "
          + "\"matterID\": \"" + rMatterUuid + "\" "
        + "} "
      + "} "
    + "}";
    log.info("sqsMessage=" + sqsMessage);

    log.info("queueEndPoint=" + queueEndPoint);
    log.info("inboundQueueName=" + inboundQueueName);

    SQSUtils sqs = new SQSUtils();
    String retrieval = sqs.pushToDelayQueue(queueEndPoint + inboundQueueName, sqsMessage);
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

  @Value("${SQS.inboundQueueName}")
  public void setQueueName(String inboundQueueName) {
    ChargeoffSqs.inboundQueueName = inboundQueueName;
  }
}

