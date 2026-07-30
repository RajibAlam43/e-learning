package com.gii.worker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gii.common.dto.SmsJobMessage;
import com.gii.common.enums.VerificationPurpose;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SmsDeliveryServiceTest {

  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void sendOtpShouldIncludeMaskingFlagAndSucceedOnJson202Response() throws Exception {
    AtomicReference<Map<String, String>> capturedForm = new AtomicReference<>(Map.of());

    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/api/smsapi",
        exchange -> {
          capturedForm.set(parseForm(exchange));
          write(exchange, 200, "{\"response_code\":202,\"success_message\":\"OK\"}");
        });
    server.start();

    SmsDeliveryService service = new SmsDeliveryService();
    ReflectionTestUtils.setField(
        service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
    ReflectionTestUtils.setField(service, "apiKey", "test-api-key");
    ReflectionTestUtils.setField(service, "senderId", "TESTSENDER");
    ReflectionTestUtils.setField(service, "timeoutMs", 3000L);

    service.sendOtp(sampleJob());

    assertThat(capturedForm.get().get("api_key")).isEqualTo("test-api-key");
    assertThat(capturedForm.get().get("number")).isEqualTo("8801811200115");
    assertThat(capturedForm.get().get("senderid")).isEqualTo("TESTSENDER");
  }

  @Test
  void sendOtpShouldThrowWhenProviderRejectsRequest() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/api/smsapi",
        exchange ->
            write(exchange, 200, "{\"response_code\":1005,\"error_message\":\"rejected\"}"));
    server.start();

    SmsDeliveryService service = new SmsDeliveryService();
    ReflectionTestUtils.setField(
        service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
    ReflectionTestUtils.setField(service, "apiKey", "test-api-key");
    ReflectionTestUtils.setField(service, "senderId", "TESTSENDER");
    ReflectionTestUtils.setField(service, "timeoutMs", 3000L);

    assertThatThrownBy(() -> service.sendOtp(sampleJob()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to send OTP SMS");
  }

  private SmsJobMessage sampleJob() {
    return SmsJobMessage.builder()
        .userId(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"))
        .toPhoneNumber("8801811200115")
        .message("Your verification code is: 123456. It will expire in 5 minutes.")
        .verificationPurpose(VerificationPurpose.PHONE_VERIFICATION)
        .verificationCode("123456")
        .createdAt(Instant.parse("2026-05-04T07:15:00Z"))
        .build();
  }

  private Map<String, String> parseForm(HttpExchange exchange) throws IOException {
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    Map<String, String> form = new HashMap<>();
    if (body.isBlank()) {
      return form;
    }
    for (String pair : body.split("&")) {
      int idx = pair.indexOf('=');
      String key =
          URLDecoder.decode(idx >= 0 ? pair.substring(0, idx) : pair, StandardCharsets.UTF_8);
      String value =
          URLDecoder.decode(idx >= 0 ? pair.substring(idx + 1) : "", StandardCharsets.UTF_8);
      form.put(key, value);
    }
    return form;
  }

  private void write(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }
}
