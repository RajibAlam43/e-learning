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

  @Value("${payments.sslcommerz.validation-base-url}")
  private String validationBaseUrl;

  @Value("${payments.sslcommerz.store-id}")
  private String storeId;

  @Value("${payments.sslcommerz.store-password}")
  private String storePassword;

  @Value("${payments.sslcommerz.validation-timeout-ms}")
  private long validationTimeoutMs;

  public void validateSuccessCallback(Order order, Map<String, String> callbackParams) {
    String tranId = callbackParams.get("tran_id");
    require(!isBlank(tranId), HttpStatus.BAD_REQUEST, "Invalid callback");
    Map<String, String> validated = validateByTranId(tranId);
    validateAgainstOrder(order, validated);
  }

  public void validateWebhookSignature(Map<String, String> callbackParams) {
    String verifyKey = callbackParams.get("verify_key");
    String verifySign = callbackParams.get("verify_sign");
    require(!isBlank(verifyKey) && !isBlank(verifySign), HttpStatus.BAD_REQUEST, "Invalid callback");

    List<String> fragments = new ArrayList<>();
    for (String key : verifyKey.split(",")) {
      String trimmed = key == null ? "" : key.trim();
      if (trimmed.isBlank()) {
        continue;
      }
      String value = callbackParams.get(trimmed);
      require(value != null, HttpStatus.BAD_REQUEST, "Invalid callback");
      fragments.add(trimmed + "=" + value);
    }
    require(!fragments.isEmpty(), HttpStatus.BAD_REQUEST, "Invalid callback");

    fragments.sort(Comparator.naturalOrder());
    String source = String.join("&", fragments) + "&store_passwd=" + md5Hex(storePassword == null ? "" : storePassword);
    String computed = md5Hex(source).toUpperCase();
    boolean valid =
        MessageDigest.isEqual(
            computed.getBytes(StandardCharsets.UTF_8), verifySign.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
    require(valid, HttpStatus.BAD_REQUEST, "Invalid callback");
  }

  private Map<String, String> validateByTranId(String expectedTranId) {
    try {
      String url =
          validationBaseUrl
              + "/validator/api/merchantTransIDvalidationAPI.php?tran_id="
              + encode(expectedTranId)
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
      require(response != null && is2xx(response.statusCode()), HttpStatus.BAD_REQUEST, "Invalid callback");

      Map<String, Object> body = objectMapper.readValue(response.body(), MAP_TYPE);
      Object elementObj = body.get("element");
      List<?> elements = elementObj instanceof List<?> list ? list : List.of();
      for (Object item : elements) {
        if (item instanceof Map<?, ?> raw) {
          @SuppressWarnings("unchecked")
          Map<String, String> candidate = (Map<String, String>) raw;
          if (VALID_STATUSES.contains(normalize(candidate.get("status")))
              && expectedTranId.equals(candidate.get("tran_id"))) {
            return candidate;
          }
        }
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    } catch (Exception ex) {
      if (ex instanceof ResponseStatusException rse) {
        throw rse;
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback", ex);
    }
  }

  private void validateAgainstOrder(Order order, Map<String, String> validated) {
    String transactionId = validated.get("tran_id");
    String currency = validated.get("currency_type");
    BigDecimal amount = new BigDecimal(validated.get("currency_amount"));
    boolean valid =
        transactionId != null
            && transactionId.equals(order.getProviderTxnId())
            && currency != null
            && currency.equalsIgnoreCase(order.getCurrency())
            && order.getAmountBdt().subtract(amount).abs().compareTo(MAX_DIFF) < 0;
    require(valid, HttpStatus.BAD_REQUEST, "Invalid callback");
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

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private boolean is2xx(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  private void require(boolean condition, HttpStatus status, String message) {
    if (!condition) {
      throw new ResponseStatusException(status, message);
    }
  }

  private record RawHttpResponse(int statusCode, String body) {}
}
