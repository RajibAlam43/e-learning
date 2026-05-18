package com.gii.api.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.dto.SslcommerzValidationJobMessage;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SslcommerzValidationJobPublisherService {

  private final SqsProducerService sqsProducerService;
  private final ObjectMapper objectMapper;

  @Value("${payments.sslcommerz.validation.jobs.queue}")
  private String validationQueue;

  @Value("${payments.sslcommerz.validation.jobs.publish-timeout-ms:3000}")
  private long publishTimeoutMs;

  public void publish(SslcommerzValidationJobMessage message) {
    final String payload;
    try {
      payload = objectMapper.writeValueAsString(message);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize SSLCommerz validation job message", e);
    }

    try {
      sqsProducerService
          .sendMessage(payload, validationQueue, null)
          .orTimeout(publishTimeoutMs, TimeUnit.MILLISECONDS)
          .join();
    } catch (CompletionException ex) {
      log.error("Failed to publish SSLCommerz validation job to SQS queue {}", validationQueue, ex);
      throw new IllegalStateException("Failed to publish SSLCommerz validation job message", ex);
    }
  }
}
