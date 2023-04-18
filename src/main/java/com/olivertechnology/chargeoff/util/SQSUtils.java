package com.olivertechnology.chargeoff.util;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SQSUtils {

  static String secretName;
  static String secretRegion;

  @Value("${SQS.secretName}")
  public void setSecretName(String secretName) {
    SQSUtils.secretName = secretName;
  }

  @Value("${SQS.secretRegion}")
  public void setSecretRegion(String secretRegion) {
    SQSUtils.secretRegion = secretRegion;
  }

  private AmazonSQS getSQSClient() {
    log.info("secretName=" + secretName);
    log.info("secretRegion=" + secretRegion);
    JSONObject secretObj = SecretManager.getSecret(secretName, secretRegion);
    String accessKey = secretObj.getAsString( "accessKey");
    String secretKey = secretObj.getAsString("secretKey");
    String clientRegion = secretObj.getAsString("clientRegion");
    log.info("accessKey=" + accessKey);
    log.info("secretKey=" + secretKey);
    log.info("clientRegion=" + clientRegion);

    BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

    AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion(clientRegion).withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

    return sqs;
  }

  public String pushToQueue(String queueURL, String json, String groupID, String uuid) {
    log.info("pushToQueue");
    log.info("queueURL=" + queueURL);
    log.info("json=" + json);
    log.info("groupID=" + groupID);
    log.info("uuid=" + uuid);

    AmazonSQS sqs = getSQSClient();

    try {
      SendMessageRequest send_msg_request = new SendMessageRequest().withQueueUrl(queueURL)
          .withMessageBody(json).withMessageGroupId(groupID).withMessageDeduplicationId(uuid);
      sqs.sendMessage(send_msg_request);
    } catch (Exception ex) {
      ex.printStackTrace();
      return "SQS Message Push failure";
    }
    return "SQS Message Pushed to Queue";
  }

  public String pushToDelayQueue(String queueURL, String json) {
    AmazonSQS sqs = getSQSClient();

    try {
      SendMessageRequest send_msg_request = new SendMessageRequest().withQueueUrl(queueURL).withMessageBody(json);
      sqs.sendMessage(send_msg_request);
    } catch (Exception ex) {
      ex.printStackTrace();
      return "SQS Message Push failure";
    }
    return "SQS Message Pushed to Queue";
  }

  public List<Message> pollQueue(String queueURL) {
    AmazonSQS sqs = getSQSClient();

    // receive messages from the queue
    List<Message> messages = sqs.receiveMessage(queueURL).getMessages();

    for (Message m : messages) {
      sqs.deleteMessage(queueURL, m.getReceiptHandle());
    }
    return messages;
  }
}

