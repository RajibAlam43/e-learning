package com.gii.api.service.util;

import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Slf4j
public class SqsProducerService {

  private final SqsAsyncClient sqsClient;
  private final Map<String, String> queueUrlCache = new ConcurrentHashMap<>();

  @Autowired
  public SqsProducerService(SqsAsyncClient sqsClient) {
    this.sqsClient = sqsClient;
  }

  public CompletableFuture<String> sendMessage(
      String message, String queueName, @Nullable String jobId) {
    jobId = jobId == null ? UUID.randomUUID().toString() : jobId;
    String queueUrl = resolveQueueUrl(queueName);

    SendMessageRequest request =
        SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(message)
            .messageAttributes(
                Map.of(
                    "jobId",
                    MessageAttributeValue.builder().dataType("String").stringValue(jobId).build()))
            .build();

    log.info("{} sent to {}", message, queueUrl);

    return sqsClient.sendMessage(request).thenApply(SendMessageResponse::messageId);
  }

  private String resolveQueueUrl(String queueName) {
    return queueUrlCache.computeIfAbsent(
        queueName,
        name ->
            sqsClient
                .getQueueUrl(GetQueueUrlRequest.builder().queueName(name).build())
                .join()
                .queueUrl());
  }
}
