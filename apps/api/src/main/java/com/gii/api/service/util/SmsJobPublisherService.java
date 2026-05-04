package com.gii.api.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.dto.SmsJobMessage;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsJobPublisherService {

  private final SqsProducerService sqsProducerService;
  private final ObjectMapper objectMapper;

  @Value("${sms.jobs.main.queue}")
  private String smsQueue;

  @Value("${sms.jobs.publish-timeout-ms}")
  private long publishTimeoutMs;

  public void publish(SmsJobMessage message) {
    final String payload;
    try {
      payload = objectMapper.writeValueAsString(message);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize SMS job message", e);
    }

    try {
      sqsProducerService
          .sendMessage(payload, smsQueue, null)
          .orTimeout(publishTimeoutMs, TimeUnit.MILLISECONDS)
          .join();
    } catch (CompletionException ex) {
      log.error("Failed to publish SMS job to SQS queue {}", smsQueue, ex);
      throw new IllegalStateException("Failed to publish SMS job message", ex);
    }
  }
}
