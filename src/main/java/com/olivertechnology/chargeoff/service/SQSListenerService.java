package com.olivertechnology.chargeoff.service;

import com.olivertechnology.chargeoff.messaging.JmsPublisher;
import com.olivertechnology.chargeoff.util.SerializationUtils;
import com.olivertechnology.chargeoff.model.dto.SqsRequest;
import com.olivertechnology.chargeoff.model.Chargeoff;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;

import com.google.gson.JsonSyntaxException;

@Service
@Slf4j
@Component
public class SQSListenerService {
  private JmsPublisher jmsPublisher;

  private String inBoundQueueName;

  @Autowired
  public SQSListenerService(final JmsPublisher jmsPublisher,
                                  @Value("${SQS.queueName}") final String inBoundQueueName) {
    this.jmsPublisher = jmsPublisher;
    this.inBoundQueueName = inBoundQueueName;
  }

  @JmsListener(destination = "${SQS.queueName}")
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

      // Call api to cancel letter
      String chargeoffRes = Chargeoff.chargeoff(matterId, chargeOffDate,
              chargeOffAmount, accountNumber);
      log.info("chargeoffRes=" + chargeoffRes);
      if (chargeoffRes != null && chargeoffRes.contains("failure")) {
        log.error("chargeoff failure");
      }
    } catch (final Exception e) {
      log.error("deserialization failed for Mail {} with error: {} {}", sqsMessage, e.getLocalizedMessage(), e);
      if (e instanceof JsonSyntaxException) {
        log.error("Invalid json " + sqsMessage);
      } else {
        log.error("re-posting message back to queue {} due to internal processing error {}", inBoundQueueName, e);
        throw new JMSException("Encounter error while processing the message.");
      }
    }
  }
}

