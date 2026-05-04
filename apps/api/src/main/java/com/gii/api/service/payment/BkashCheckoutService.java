package com.gii.api.service.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.entity.order.Order;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BkashCheckoutService {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final ObjectMapper objectMapper;
  private final WebClient.Builder webClientBuilder;

  @Value("${payments.bkash.base-url}")
  private String baseUrl;

  @Value("${payments.bkash.username}")
  private String username;

  @Value("${payments.bkash.password}")
  private String password;

  @Value("${payments.bkash.app-key}")
  private String appKey;

  @Value("${payments.bkash.app-secret}")
  private String appSecret;

  @Value("${payments.bkash.intent:sale}")
  private String intent;

  @Value("${payments.bkash.timeout-ms:10000}")
  private long timeoutMs;

  private String idToken;
  private String refreshToken;
  private Instant tokenExpiresAt;

  public CreatePaymentResult createPayment(Order order) {
    ensureCredentials();
    Map<String, Object> response =
        call(
            "/checkout/payment/create",
            HttpMethod.POST,
            Map.of(
                "amount", order.getAmountBdt().toPlainString(),
                "currency", order.getCurrency(),
                "intent", intent,
                "merchantInvoiceNumber", order.getId().toString()));
    String paymentId = asString(response.get("paymentID"));
    require(!isBlank(paymentId), HttpStatus.BAD_REQUEST, "Invalid payment response");
    return new CreatePaymentResult(
        paymentId, asString(firstNonNull(response.get("bkashURL"), response.get("paymentURL"))));
  }

  public Map<String, Object> executePayment(String paymentId) {
    ensureCredentials();
    return call("/checkout/payment/execute/" + paymentId, HttpMethod.POST, Map.of());
  }

  public Map<String, Object> queryPayment(String paymentId) {
    ensureCredentials();
    return call("/checkout/payment/query/" + paymentId, HttpMethod.GET, null);
  }

  public void validateSuccessCallback(Order order, Map<String, String> callbackParams) {
    ensureCredentials();

    String paymentId =
        firstNonBlank(
            callbackParams.get("payment_id"),
            callbackParams.get("paymentID"),
            callbackParams.get("tran_id"));
    require(!isBlank(paymentId), HttpStatus.BAD_REQUEST, "Invalid callback");
    require(
        order.getProviderTxnId() == null || order.getProviderTxnId().equals(paymentId),
        HttpStatus.BAD_REQUEST,
        "Invalid callback");

    Map<String, Object> verified = executePayment(paymentId);
    String transactionStatus = asString(verified.get("transactionStatus"));
    if (!"Completed".equalsIgnoreCase(transactionStatus)) {
      verified = queryPayment(paymentId);
      transactionStatus = asString(verified.get("transactionStatus"));
    }
    require("Completed".equalsIgnoreCase(transactionStatus), HttpStatus.BAD_REQUEST, "Invalid callback");

    String amount = asString(verified.get("amount"));
    String currency = asString(verified.get("currency"));
    String invoice = asString(verified.get("merchantInvoiceNumber"));
    require(!isBlank(amount), HttpStatus.BAD_REQUEST, "Invalid callback");
    require(new BigDecimal(amount).compareTo(order.getAmountBdt()) == 0, HttpStatus.BAD_REQUEST, "Invalid callback");
    require(!isBlank(currency) && currency.equalsIgnoreCase(order.getCurrency()), HttpStatus.BAD_REQUEST, "Invalid callback");
    require(isBlank(invoice) || invoice.equals(order.getId().toString()), HttpStatus.BAD_REQUEST, "Invalid callback");
  }

  private Map<String, Object> call(String path, HttpMethod method, Map<String, Object> payload) {
    try {
      RawHttpResponse response = sendWithAuthRetry(path, method, payload);
      require(response != null, HttpStatus.BAD_REQUEST, "bKash API call failed");
      require(is2xx(response.statusCode()), HttpStatus.BAD_REQUEST, "bKash API call failed");
      Map<String, Object> parsed = objectMapper.readValue(response.body(), MAP_TYPE);
      String statusCode = asString(parsed.get("statusCode"));
      require(isBlank(statusCode) || "0000".equals(statusCode), HttpStatus.BAD_REQUEST, "bKash API call failed");
      return parsed;
    } catch (Exception ex) {
      if (ex instanceof ResponseStatusException rse) {
        throw rse;
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bKash API call failed", ex);
    }
  }

  private void ensureCredentials() {
    require(
        !isBlank(baseUrl) && !isBlank(username) && !isBlank(password) && !isBlank(appKey) && !isBlank(appSecret),
        HttpStatus.SERVICE_UNAVAILABLE,
        "bKash is not configured");
  }

  private synchronized Map<String, String> authenticatedHeaders() {
    Instant now = Instant.now();
    if (!isBlank(idToken) && tokenExpiresAt != null && tokenExpiresAt.isAfter(now.plusSeconds(30))) {
      return Map.of("Authorization", idToken);
    }

    if (!isBlank(refreshToken)) {
      try {
        refreshToken();
        return Map.of("Authorization", idToken);
      } catch (Exception ignored) {
      }
    }

    grantToken();
    return Map.of("Authorization", idToken);
  }

  private void grantToken() {
    cacheTokenResponse(tokenPost("/checkout/token/grant", Map.of("app_key", appKey, "app_secret", appSecret)));
  }

  private void refreshToken() {
    cacheTokenResponse(
        tokenPost(
            "/checkout/token/refresh",
            Map.of("app_key", appKey, "app_secret", appSecret, "refresh_token", refreshToken)));
  }

  private Map<String, Object> tokenPost(String path, Map<String, Object> payload) {
    try {
      RawHttpResponse response =
          webClient(path, HttpMethod.POST)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .header("username", username)
              .header("password", password)
              .bodyValue(payload)
              .exchangeToMono(
                  clientResponse ->
                      clientResponse
                          .bodyToMono(String.class)
                          .defaultIfEmpty("")
                          .map(body -> new RawHttpResponse(clientResponse.statusCode().value(), body)))
              .block(Duration.ofMillis(timeoutMs));
      require(response != null, HttpStatus.BAD_REQUEST, "bKash token fetch failed");
      require(is2xx(response.statusCode()), HttpStatus.BAD_REQUEST, "bKash token fetch failed");
      Map<String, Object> parsed = objectMapper.readValue(response.body(), MAP_TYPE);
      String statusCode = asString(parsed.get("statusCode"));
      require(isBlank(statusCode) || "0000".equals(statusCode), HttpStatus.BAD_REQUEST, "bKash token fetch failed");
      return parsed;
    } catch (Exception ex) {
      if (ex instanceof ResponseStatusException rse) {
        throw rse;
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bKash token fetch failed", ex);
    }
  }

  private void cacheTokenResponse(Map<String, Object> response) {
    String newIdToken = asString(response.get("id_token"));
    require(!isBlank(newIdToken), HttpStatus.BAD_REQUEST, "bKash token fetch failed");

    this.idToken = newIdToken;
    String refreshed = asString(response.get("refresh_token"));
    if (!isBlank(refreshed)) {
      this.refreshToken = refreshed;
    }
    this.tokenExpiresAt = Instant.now().plusSeconds(Math.max(30, parseExpiresIn(asString(response.get("expires_in")))));
  }

  private long parseExpiresIn(String expiresInRaw) {
    try {
      return isBlank(expiresInRaw) ? 300 : Long.parseLong(expiresInRaw);
    } catch (Exception ex) {
      return 300;
    }
  }

  private RawHttpResponse sendWithAuthRetry(String path, HttpMethod method, Map<String, Object> payload)
      throws Exception {
    RawHttpResponse response = sendAuthenticated(path, method, payload);
    if (response.statusCode() == 401) {
      invalidateTokenCache();
      response = sendAuthenticated(path, method, payload);
    }
    return response;
  }

  private RawHttpResponse sendAuthenticated(String path, HttpMethod method, Map<String, Object> payload)
      throws Exception {
    WebClient.RequestBodySpec request =
        webClient(path, method)
            .accept(MediaType.APPLICATION_JSON)
            .header("Authorization", authenticatedHeaders().get("Authorization"))
            .header("X-APP-Key", appKey);
    WebClient.RequestHeadersSpec<?> spec =
        payload == null ? request : request.contentType(MediaType.APPLICATION_JSON).bodyValue(payload);
    return spec
        .exchangeToMono(
            clientResponse ->
                clientResponse
                    .bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> new RawHttpResponse(clientResponse.statusCode().value(), body)))
        .block(Duration.ofMillis(timeoutMs));
  }

  private synchronized void invalidateTokenCache() {
    this.idToken = null;
    this.tokenExpiresAt = null;
  }

  public record CreatePaymentResult(String paymentId, String bkashUrl) {}

  private record RawHttpResponse(int statusCode, String body) {}

  private WebClient.RequestBodySpec webClient(String path, HttpMethod method) {
    return webClientBuilder.baseUrl(baseUrl).build().method(method).uri(path);
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private Object firstNonNull(Object first, Object second) {
    return first != null ? first : second;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (!isBlank(value)) {
        return value;
      }
    }
    return null;
  }

  private boolean is2xx(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private void require(boolean condition, HttpStatus status, String message) {
    if (!condition) {
      throw new ResponseStatusException(status, message);
    }
  }
}
