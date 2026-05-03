package com.gii.api.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.dto.EmailJobMessage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailJobPublisherService {

  private final SqsProducerService sqsProducerService;
  private final ObjectMapper objectMapper;

  @Value("${email.jobs.main.queue}")
  private String emailQueue;

  @Value("${email.jobs.publish-timeout-ms:5000}")
  private long publishTimeoutMs;

  public void publish(EmailJobMessage message) {
    final String payload;
    try {
      payload = objectMapper.writeValueAsString(message);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize email job message", e);
    }

    try {
      sqsProducerService
          .sendMessage(payload, emailQueue, null)
          .orTimeout(publishTimeoutMs, TimeUnit.MILLISECONDS)
          .join();
    } catch (CompletionException ex) {
      log.error("Failed to publish email job to SQS queue {}", emailQueue, ex);
      throw new IllegalStateException("Failed to publish email job message", ex);
    }
  }
}
