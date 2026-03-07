package com.cognizant.lms.userservice.messaging;

import com.cognizant.lms.userservice.config.SqsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
public class SqsProducerService {

  private final SqsClient sqsClient;
  private final String queueUrl;

  public SqsProducerService(SqsConfig sqsConfig, @Value("${AWS_SQS_QUEUE_URL}") String queueUrl) {
    this.sqsClient = sqsConfig.sqsClient();
    this.queueUrl = queueUrl;
  }

  public SendMessageResponse sendMessage(String message) {
    SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
        .queueUrl(queueUrl)
        .messageBody(message)
        .build();
    SendMessageResponse sendMsgResponse = sqsClient.sendMessage(sendMsgRequest);
    return sendMsgResponse;
  }
}
