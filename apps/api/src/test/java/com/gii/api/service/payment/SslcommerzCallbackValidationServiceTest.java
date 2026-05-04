package com.gii.api.service.payment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.entity.order.Order;
import com.gii.common.enums.OrderProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

class SslcommerzCallbackValidationServiceTest {

  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void strictValidationShouldRejectLargeAmountMismatch() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/validator/api/validationserverAPI.php",
        exchange ->
            writeJson(
                exchange,
                200,
                "{\"status\":\"VALID\",\"tran_id\":\"txn-1\",\"currency_type\":\"BDT\",\"amount\":\"1000.50\"}"));
    server.start();

    SslcommerzCallbackValidationService service = buildService();
    ReflectionTestUtils.setField(
        service,
        "validationApiUrl",
        "http://127.0.0.1:" + server.getAddress().getPort() + "/validator/api/validationserverAPI.php");

    Order order = sampleOrder("txn-1", new BigDecimal("1000.00"));
    Map<String, String> params = callbackParams("txn-1", "val-1", "1000.00", "BDT", "VALID");

    assertThatThrownBy(() -> service.validateSuccessCallback(order, params))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void strictValidationShouldRequireBaseUrl() {
    SslcommerzCallbackValidationService service = buildService();
    ReflectionTestUtils.setField(service, "validationApiUrl", "");

    Order order = sampleOrder("txn-1", new BigDecimal("1000.00"));
    Map<String, String> params = callbackParams("txn-1", "val-1", "1000.00", "BDT", "VALID");

    assertThatThrownBy(() -> service.validateSuccessCallback(order, params))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
  }

  @Test
  void webhookSignatureShouldValidateWithVerifyKeyAndVerifySign() {
    SslcommerzCallbackValidationService service = buildService();
    Map<String, String> params = new HashMap<>();
    params.put("status", "VALID");
    params.put("tran_id", "txn-1");
    params.put("val_id", "val-1");
    params.put("verify_key", "status,tran_id,val_id");
    params.put("verify_sign", sign(params, "store-pass"));

    service.validateWebhookSignature(params);
  }

  @Test
  void webhookSignatureShouldRejectInvalidVerifySign() {
    SslcommerzCallbackValidationService service = buildService();
    Map<String, String> params = new HashMap<>();
    params.put("status", "VALID");
    params.put("tran_id", "txn-1");
    params.put("val_id", "val-1");
    params.put("verify_key", "status,tran_id,val_id");
    params.put("verify_sign", "invalid-sign");

    assertThatThrownBy(() -> service.validateWebhookSignature(params))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST));
  }

  private SslcommerzCallbackValidationService buildService() {
    SslcommerzCallbackValidationService service =
        new SslcommerzCallbackValidationService(new ObjectMapper(), WebClient.builder());
    ReflectionTestUtils.setField(service, "validationApiUrl", "http://127.0.0.1:0/validator/api/validationserverAPI.php");
    ReflectionTestUtils.setField(service, "storeId", "store-id");
    ReflectionTestUtils.setField(service, "storePassword", "store-pass");
    ReflectionTestUtils.setField(service, "validationTimeoutMs", 3000L);
    return service;
  }

  private Map<String, String> callbackParams(
      String tranId, String valId, String amount, String currency, String status) {
    Map<String, String> params = new HashMap<>();
    params.put("tran_id", tranId);
    params.put("val_id", valId);
    params.put("status", status);
    params.put("amount", amount);
    params.put("currency", currency);
    return params;
  }

  private Order sampleOrder(String providerTxnId, BigDecimal amount) {
    Order order = Order.builder().amountBdt(amount).provider(OrderProvider.SSLCOMMERZ).build();
    order.setId(UUID.randomUUID());
    order.setCurrency("BDT");
    order.setProviderTxnId(providerTxnId);
    return order;
  }

  private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  private String sign(Map<String, String> params, String storePassword) {
    String verifyKey = params.get("verify_key");
    List<String> fragments = new ArrayList<>();
    for (String key : verifyKey.split(",")) {
      String trimmed = key.trim();
      fragments.add(trimmed + "=" + params.get(trimmed));
    }
    fragments.sort(Comparator.naturalOrder());
    String source =
        String.join("&", fragments) + "&store_passwd=" + md5Hex(storePassword);
    return md5Hex(source).toUpperCase();
  }

  private String md5Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
