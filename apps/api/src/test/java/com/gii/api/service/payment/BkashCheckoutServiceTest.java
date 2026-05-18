package com.gii.api.service.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.api.service.payment.bkash.BkashCheckoutService;
import com.gii.common.entity.order.Order;
import com.gii.common.enums.OrderProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

class BkashCheckoutServiceTest {

  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void createPaymentShouldGrantTokenAndUseIt() throws Exception {
    AtomicInteger grantCalls = new AtomicInteger(0);
    AtomicInteger createCalls = new AtomicInteger(0);
    AtomicReference<String> createRequestBody = new AtomicReference<>();

    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/checkout/token/grant",
        exchange -> {
          grantCalls.incrementAndGet();
          writeJson(
              exchange,
              200,
              "{\"id_token\":\"token-1\",\"refresh_token\":\"refresh-1\",\"expires_in\":\"3600\"}");
        });
    server.createContext(
        "/checkout/payment/create",
        exchange -> {
          createCalls.incrementAndGet();
          createRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          String auth = exchange.getRequestHeaders().getFirst("Authorization");
          if (!"token-1".equals(auth)) {
            writeJson(exchange, 401, "{\"statusCode\":\"401\"}");
            return;
          }
          writeJson(
              exchange,
              200,
              "{\"statusCode\":\"0000\",\"paymentID\":\"pay-1\",\"bkashURL\":\"https://pay.example\"}");
        });
    server.start();

    BkashCheckoutService service = buildService();
    ReflectionTestUtils.setField(
        service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());

    BkashCheckoutService.CreatePaymentResult result = service.createPayment(sampleOrder());

    assertThat(result.paymentId()).isEqualTo("pay-1");
    assertThat(result.bkashUrl()).isEqualTo("https://pay.example");
    assertThat(grantCalls.get()).isEqualTo(1);
    assertThat(createCalls.get()).isEqualTo(1);
    Map<?, ?> payload = new ObjectMapper().readValue(createRequestBody.get(), Map.class);
    assertThat(payload.get("mode")).isEqualTo("0011");
    assertThat(payload.get("successCallbackURL"))
        .isEqualTo("https://stage-api.globalislamicinstitute.com/payments/bkash/" + sampleOrderId() + "/success");
    assertThat(payload.get("failureCallbackURL"))
        .isEqualTo("https://stage-api.globalislamicinstitute.com/payments/bkash/" + sampleOrderId() + "/failed");
    assertThat(payload.get("cancelledCallbackURL"))
        .isEqualTo("https://stage-api.globalislamicinstitute.com/payments/bkash/" + sampleOrderId() + "/cancelled");
  }

  @Test
  void createPaymentShouldRetryAfter401UsingRefreshToken() throws Exception {
    AtomicInteger grantCalls = new AtomicInteger(0);
    AtomicInteger refreshCalls = new AtomicInteger(0);
    AtomicInteger createCalls = new AtomicInteger(0);

    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/checkout/token/grant",
        exchange -> {
          grantCalls.incrementAndGet();
          writeJson(
              exchange,
              200,
              "{\"id_token\":\"token-1\",\"refresh_token\":\"refresh-1\",\"expires_in\":\"3600\"}");
        });
    server.createContext(
        "/checkout/token/refresh",
        exchange -> {
          refreshCalls.incrementAndGet();
          writeJson(
              exchange,
              200,
              "{\"id_token\":\"token-2\",\"refresh_token\":\"refresh-2\",\"expires_in\":\"3600\"}");
        });
    server.createContext(
        "/checkout/payment/create",
        exchange -> {
          createCalls.incrementAndGet();
          String auth = exchange.getRequestHeaders().getFirst("Authorization");
          if ("token-1".equals(auth)) {
            writeJson(exchange, 401, "{\"statusCode\":\"401\"}");
            return;
          }
          if ("token-2".equals(auth)) {
            writeJson(
                exchange,
                200,
                "{\"statusCode\":\"0000\",\"paymentID\":\"pay-2\",\"bkashURL\":\"https://pay.example/2\"}");
            return;
          }
          writeJson(exchange, 400, "{\"statusCode\":\"400\"}");
        });
    server.start();

    BkashCheckoutService service = buildService();
    ReflectionTestUtils.setField(
        service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());

    service.createPayment(sampleOrder());
    BkashCheckoutService.CreatePaymentResult second = service.createPayment(sampleOrder());

    assertThat(second.paymentId()).isEqualTo("pay-2");
    assertThat(grantCalls.get()).isEqualTo(1);
    assertThat(refreshCalls.get()).isEqualTo(1);
    assertThat(createCalls.get()).isEqualTo(3);
  }

  private BkashCheckoutService buildService() {
    BkashCheckoutService service =
        new BkashCheckoutService(new ObjectMapper(), WebClient.builder());
    ReflectionTestUtils.setField(service, "baseUrl", "http://127.0.0.1:0");
    ReflectionTestUtils.setField(service, "username", "u");
    ReflectionTestUtils.setField(service, "password", "p");
    ReflectionTestUtils.setField(service, "appKey", "app-key");
    ReflectionTestUtils.setField(service, "appSecret", "app-secret");
    ReflectionTestUtils.setField(service, "timeoutMs", 3000L);
    ReflectionTestUtils.setField(service, "redirectSubdomain", "stage-api");
    return service;
  }

  private Order sampleOrder() {
    Order order =
        Order.builder().amountBdt(new BigDecimal("1500.00")).provider(OrderProvider.BKASH).build();
    order.setId(sampleOrderId());
    order.setCurrency("BDT");
    return order;
  }

  private UUID sampleOrderId() {
    return UUID.fromString("6d949b5c-c510-48e2-a5db-689743ab40ff");
  }

  private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }
}
