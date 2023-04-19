package com.olivertechnology.chargeoff.service;

import com.olivertechnology.chargeoff.messaging.JmsPublisher;
import com.olivertechnology.chargeoff.util.SerializationUtils;
import com.olivertechnology.chargeoff.model.Chargeoff;
import com.olivertechnology.chargeoff.model.ChargeoffReversal;
import com.olivertechnology.chargeoff.model.dto.SqsRequest;
import com.olivertechnology.chargeoff.model.dto.ChargeOffResponse;
import com.olivertechnology.chargeoff.model.dto.Envelope;
import com.olivertechnology.chargeoff.model.dto.MessageBody;
import com.olivertechnology.chargeoff.model.dto.ResponseBody;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonSyntaxException;

@Service
@Slf4j
@Component
public class SQSListenerService {
  private JmsPublisher jmsPublisher;

  private String inBoundQueueName;
  private String outBoundQueueName;

  private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  @Autowired
  public SQSListenerService(final JmsPublisher jmsPublisher,
                                  @Value("${SQS.inboundQueueName}") final String inBoundQueueName,
                                  @Value("${SQS.outboundQueueName}") final String outBoundQueueName
                            ) {
    this.jmsPublisher = jmsPublisher;
    this.inBoundQueueName = inBoundQueueName;
    this.outBoundQueueName = outBoundQueueName;
  }

  @JmsListener(destination = "${SQS.inboundQueueName}")
  public void processSQSMessages(final String sqsMessage) throws JMSException {
    log.info("processSQSMessages");
    log.info("messages from SQS {} ", sqsMessage);

    try {
      final SqsRequest sqsRequest = SerializationUtils.deserialize(sqsMessage, SqsRequest.class);

      // Process message
      String requestId = sqsRequest.getEnvelope().getRequestID();
      log.info("requestId=" + requestId);
      String function = sqsRequest.getEnvelope().getEventType();
      log.info("function=" + function);
      String matterId = sqsRequest.getRequestBody().getPayload().getMatterID();
      log.info("matterId=" + matterId);
      String chargeOffDate = sqsRequest.getRequestBody().getPayload().getChargeOffDate();
      log.info("chargeOffDate=" + chargeOffDate);
      String chargeOffAmount = sqsRequest.getRequestBody().getPayload().getChargeOffAmount();
      log.info("chargeOffAmount=" + chargeOffAmount);
      String accountNumber = sqsRequest.getRequestBody().getAccountNumber();
      log.info("accountNumber=" + accountNumber);
      String httpMethod = sqsRequest.getEnvelope().getHttpMethod();
      log.info("httpMethod=" + httpMethod);
      String eventType = sqsRequest.getEnvelope().getEventType();
      log.info("eventType=" + eventType);
      String customer = sqsRequest.getEnvelope().getCustomer();
      log.info("customer=" + customer);

      int statusCode = HttpStatus.OK.value();
      String message = "success";

      // Check event type here
      if (eventType == null) {
        statusCode = HttpStatus.NO_CONTENT.value();
        message = "No eventType found";
      } else if (eventType.equals("CHARGE_OFF")) {
        // Call api to do chargeoff
        String chargeoffRes = Chargeoff.chargeoff(matterId, chargeOffDate,
                chargeOffAmount, accountNumber);
        log.info("chargeoffRes=" + chargeoffRes);
        if (chargeoffRes != null && chargeoffRes.contains("failure")) {
          log.error("chargeoff failure");
          statusCode = HttpStatus.BAD_REQUEST.value();
          message = "failure";
        }
      } else if (eventType.equals("CHARGE_OFF_REVERSAL")) {
        // Call api to do chargeoff reversal
        String chargeoffRes = ChargeoffReversal.chargeoffReversal(matterId, accountNumber);
        log.info("chargeoffRes=" + chargeoffRes);
        if (chargeoffRes != null && chargeoffRes.contains("failure")) {
          log.error("chargeoff reversal failure");
          statusCode = HttpStatus.BAD_REQUEST.value();
          message = "failure";
        }
      } else {
        statusCode = HttpStatus.NOT_FOUND.value();
        message = "Unknown eventType " + eventType;
      }

      // Generate outbound message back when chargeoff is completed.
      sendResponse(matterId, accountNumber, requestId, eventType, httpMethod, statusCode, customer, message);
    } catch (final Exception e) {
      log.error("deserialization failed for Mail {} with error: {} {}", sqsMessage, e.getLocalizedMessage(), e);
      if (e instanceof JsonSyntaxException) {
        log.error("Invalid json " + sqsMessage);
      } else {
        log.error("re-posting message back to queue {} due to internal processing error {}", inBoundQueueName, e);
        throw new JMSException("Encounter error while processing the message.");
      }
    } finally {
      // Generate failure outbound message
      String matterId = parseMatterId(sqsMessage);
      String requestId = parseRequestId(sqsMessage);
      String accountNumber = parseAccountNumber(sqsMessage);
      String eventType = parseEventType(sqsMessage);
      sendResponse(matterId, accountNumber, requestId, eventType, "POST", HttpStatus.INTERNAL_SERVER_ERROR.value(),
              "", "failure");
    }
  }

  private void sendResponse(String matterId, String accountNumber, String requestId, String eventType,
                            String httpMethod, int statusCode, String customer, String message) {

    final String timestamp = ZonedDateTime.now().format(formatter);
    jmsPublisher.publishObject(outBoundQueueName, ChargeOffResponse.builder()
            .envelope(Envelope.builder()
                    .httpMethod(httpMethod)
                    .eventType(eventType)
                    .customer(customer)
                    .requestID(requestId)
                    .timestamp(timestamp)
                    .build())
            .requestBody(ResponseBody.builder()
                    .statusCode(statusCode)
                    .body(MessageBody.builder()
                            .eventName(eventType)
                            .eventDate(timestamp)
                            .accountNumber(accountNumber)
                            .matterID(matterId)
                            .message(message)
                            .build())
                    .build()).build());
  }

  private String parseMatterId(final String sqsMessage) {
    try{
      final String fromRequestId = sqsMessage.substring(sqsMessage.contains("\"matterID\":") ? sqsMessage.indexOf("\"matterID\":"):0);
      return fromRequestId.substring(0,fromRequestId.indexOf(",")).replace("\"matterID\":","").replace("\"","");
    } catch (final Exception ex) {
      log.info("Failed to get matterID from chargeOffRequest {}",ex.getLocalizedMessage(),ex);
    }
    return "";
  }

  private String parseRequestId(final String sqsMessage) {
    try{
      final String fromRequestId = sqsMessage.substring(sqsMessage.contains("\"requestID\":") ? sqsMessage.indexOf("\"requestID\":"):0);
      return fromRequestId.substring(0,fromRequestId.indexOf(",")).replace("\"requestID\":","").replace("\"","");
    } catch (final Exception ex) {
      log.info("Failed to get requestID from chargeOffRequest {}",ex.getLocalizedMessage(),ex);
    }
    return "";
  }

  private String parseAccountNumber(final String sqsMessage) {
    try {
      final String fromRequestId = sqsMessage.substring(sqsMessage.contains("\"accountNumber\":") ?sqsMessage.indexOf("\"accountNumber\":"):0);
      return fromRequestId.substring(0,fromRequestId.indexOf(",")).replace("\"accountNumber\":","").replace("\"","");
    } catch (final Exception ex) {
      log.info("Failed to get accountNumber from chargeOffRequest {}",ex.getLocalizedMessage(),ex);
    }
    return "";
  }

  private String parseEventType(final String sqsMessage) {
    try {
      final String fromRequestId = sqsMessage.substring(sqsMessage.contains("\"eventType\":") ?sqsMessage.indexOf("\"eventType\":"):0);
      return fromRequestId.substring(0,fromRequestId.indexOf(",")).replace("\"eventType\":","").replace("\"","");
    } catch (final Exception ex) {
      log.info("Failed to get eventType from chargeOffRequest {}",ex.getLocalizedMessage(),ex);
    }
    return "";
  }
}

