package com.gii.worker.service;

import com.gii.common.dto.SmsJobMessage;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class SmsDeliveryService {

  @Value("${sms.provider.bulksmsbd.base-url}")
  private String baseUrl;

  @Value("${sms.provider.bulksmsbd.api-key}")
  private String apiKey;

  @Value("${sms.provider.bulksmsbd.sender-id}")
  private String senderId;

  @Value("${sms.provider.bulksmsbd.timeout-ms}")
  private long timeoutMs;

  private final WebClient webClient;

  public SmsDeliveryService() {
    this.webClient = WebClient.builder().build();
  }

  public void sendOtp(SmsJobMessage job) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("SMS provider API key is not configured");
    }
    if (senderId == null || senderId.isBlank()) {
      throw new IllegalStateException("SMS provider sender id is not configured");
    }

    String endpoint = baseUrl + "/api/smsapi";

    try {
      String payload =
          webClient
              .post()
              .uri(endpoint)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(
                  BodyInserters.fromFormData("api_key", apiKey)
                      .with("type", "text")
                      .with("number", job.toPhoneNumber())
                      .with("senderid", senderId)
                      .with("message", job.message()))
              .retrieve()
              .bodyToMono(String.class)
              .block(Duration.ofMillis(timeoutMs));
      payload = payload == null ? "" : payload.trim();
      if (!isSuccess(payload)) {
        throw new IllegalStateException("SMS provider rejected request. response=" + payload);
      }
    } catch (Exception e) {
      log.error("Failed to send OTP SMS to {}", job.toPhoneNumber(), e);
      throw new IllegalStateException("Failed to send OTP SMS", e);
    }
  }

  private boolean isSuccess(String payload) {
    return payload.startsWith("202")
        || payload.contains("\"response_code\":202")
        || payload.contains("\"response_code\":\"202\"");
  }
}
