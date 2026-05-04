package com.gii.api.service.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.entity.order.Order;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SslcommerzCallbackValidationService {

  private static final Set<String> VALID_STATUSES = Set.of("VALID", "VALIDATED");
  private static final BigDecimal MAX_DIFF = new BigDecimal("0.1");
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final ObjectMapper objectMapper;
  private final WebClient.Builder webClientBuilder;

  @Value("${payments.sslcommerz.validation-api-url}")
  private String validationApiUrl;

  @Value("${payments.sslcommerz.store-id}")
  private String storeId;

  @Value("${payments.sslcommerz.store-password}")
  private String storePassword;

  @Value("${payments.sslcommerz.validation-timeout-ms}")
  private long validationTimeoutMs;

  public void validateSuccessCallback(Order order, Map<String, String> callbackParams) {
    String valId = callbackParams.get("val_id");
    if (isBlank(valId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    }
    validateAgainstOrder(order, validateByValId(valId));
  }

  public void validateWebhookSignature(Map<String, String> callbackParams) {
    String verifyKey = callbackParams.get("verify_key");
    String verifySign = callbackParams.get("verify_sign");
    if (isBlank(verifyKey) || isBlank(verifySign)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    }

    List<String> fragments = new ArrayList<>();
    for (String key : verifyKey.split(",")) {
      String trimmed = key == null ? "" : key.trim();
      if (trimmed.isBlank()) {
        continue;
      }
      String value = callbackParams.get(trimmed);
      if (value == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
      }
      fragments.add(trimmed + "=" + value);
    }
    if (fragments.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    }

    fragments.sort(Comparator.naturalOrder());
    String source = String.join("&", fragments) + "&store_passwd=" + md5Hex(storePassword == null ? "" : storePassword);
    String computed = md5Hex(source).toUpperCase();
    boolean valid =
        MessageDigest.isEqual(
            computed.getBytes(StandardCharsets.UTF_8), verifySign.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
    if (!valid) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    }
  }

  private Map<String, Object> validateByValId(String valId) {
    if (isBlank(validationApiUrl) || isBlank(storeId) || isBlank(storePassword)) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SSLCommerz is not configured");
    }
    try {
      String url =
          validationApiUrl
              + "?val_id="
              + encode(valId)
              + "&store_id="
              + encode(storeId)
              + "&store_passwd="
              + encode(storePassword)
              + "&v=1&format=json";
      RawHttpResponse response =
          webClientBuilder
              .build()
              .get()
              .uri(url)
              .exchangeToMono(
                  clientResponse ->
                      clientResponse
                          .bodyToMono(String.class)
                          .defaultIfEmpty("")
                          .map(body -> new RawHttpResponse(clientResponse.statusCode().value(), body)))
              .block(Duration.ofMillis(validationTimeoutMs));
      if (response == null || response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
      }
      return objectMapper.readValue(response.body(), MAP_TYPE);
    } catch (Exception ex) {
      if (ex instanceof ResponseStatusException rse) {
        throw rse;
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback", ex);
    }
  }

  private void validateAgainstOrder(Order order, Map<String, Object> validated) {
    String status = normalize(asString(validated.get("status")));
    String tranId = asString(validated.get("tran_id"));
    String currency = asString(validated.get("currency_type"));
    if (isBlank(currency)) {
      currency = asString(validated.get("currency"));
    }
    String amountRaw = asString(validated.get("amount"));
    if (isBlank(amountRaw)) {
      amountRaw = asString(validated.get("currency_amount"));
    }
    if (isBlank(amountRaw)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    }
    BigDecimal amount = new BigDecimal(amountRaw);

    boolean valid =
        VALID_STATUSES.contains(status)
            && tranId != null
            && tranId.equals(order.getProviderTxnId())
            && currency != null
            && currency.equalsIgnoreCase(order.getCurrency())
            && order.getAmountBdt().subtract(amount).abs().compareTo(MAX_DIFF) < 0;
    if (!valid) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    }
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase();
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
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
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Signature check failed");
    }
  }

  private record RawHttpResponse(int statusCode, String body) {}
}
