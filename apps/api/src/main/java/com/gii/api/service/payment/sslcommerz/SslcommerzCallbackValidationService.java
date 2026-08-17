package com.gii.api.service.payment.sslcommerz;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
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
    ValidationOutcome outcome = validateIpnNotification(order, callbackParams);
    if (!VALID_STATUSES.contains(outcome.status())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    }
  }

  public ValidationOutcome validateIpnNotification(
      Order order, Map<String, String> callbackParams) {
    String valId = callbackParams.get("val_id");
    if (isBlank(valId)) {
      log.warn(
          "SSLCommerz validation failed: missing val_id; orderId={}, providerTxnId={}, callbackTranId={}",
          order.getId(),
          order.getProviderTxnId(),
          callbackParams.get("tran_id"));
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    }
    Map<String, Object> validated = validateByValId(valId);
    validateAgainstOrder(order, validated);
    return new ValidationOutcome(
        normalize(asString(validated.get("status"))),
        parseRiskLevel(asString(validated.get("risk_level"))));
  }

  public void validateWebhookSignature(Map<String, String> callbackParams) {
    String verifyKey = callbackParams.get("verify_key");
    String verifySign = callbackParams.get("verify_sign");
    if (isBlank(verifyKey) || isBlank(verifySign)) {
      log.warn(
          "SSLCommerz signature validation failed: missing verify fields; verify_key_present={}, verify_sign_present={}, tran_id={}, val_id={}",
          !isBlank(verifyKey),
          !isBlank(verifySign),
          callbackParams.get("tran_id"),
          callbackParams.get("val_id"));
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    }

    List<String> keys = new ArrayList<>();
    Map<String, String> valuesByKey = new java.util.HashMap<>();
    for (String key : verifyKey.split(",")) {
      String trimmed = key == null ? "" : key.trim();
      if (trimmed.isBlank()) {
        continue;
      }
      String value = callbackParams.get(trimmed);
      if (value == null) {
        value = "";
      }
      keys.add(trimmed);
      valuesByKey.put(trimmed, value);
    }
    if (keys.isEmpty()) {
      log.warn(
          "SSLCommerz signature validation failed: verify fragments empty; tran_id={}, val_id={}",
          callbackParams.get("tran_id"),
          callbackParams.get("val_id"));
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    }

    keys.sort(Comparator.naturalOrder());
    List<String> fragments = new ArrayList<>(keys.size());
    for (String key : keys) {
      String value = valuesByKey.getOrDefault(key, "");
      fragments.add(key + "=" + value);
    }

    List<String> encodedFragments = new ArrayList<>(keys.size());
    for (String key : keys) {
      String value = valuesByKey.getOrDefault(key, "");
      encodedFragments.add(key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    String safeStorePassword = storePassword == null ? "" : storePassword;
    String md5Password = md5Hex(safeStorePassword);

    String computedDecodedWithMd5Password = computeSignature(fragments, md5Password);
    String computedEncodedWithMd5Password = computeSignature(encodedFragments, md5Password);
    String computedDecodedWithRawPassword = computeSignature(fragments, safeStorePassword);
    String computedEncodedWithRawPassword = computeSignature(encodedFragments, safeStorePassword);

    boolean valid = equalsSignature(computedDecodedWithMd5Password, verifySign);
    if (!valid) {
      log.info("Option 1 of password did not work");
      valid = equalsSignature(computedEncodedWithMd5Password, verifySign);
    }
    if (!valid) {
      log.info("Option 2 of password did not work");
      valid = equalsSignature(computedDecodedWithRawPassword, verifySign);
    }
    if (!valid) {
      log.info("Option 3 of password did not work");
      valid = equalsSignature(computedEncodedWithRawPassword, verifySign);
    }

    if (!valid) {
      log.warn(
          "SSLCommerz signature mismatch; tran_id={}, val_id={}",
          callbackParams.get("tran_id"),
          callbackParams.get("val_id"));
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    }
  }

  private Map<String, Object> validateByValId(String valId) {
    if (isBlank(validationApiUrl) || isBlank(storeId) || isBlank(storePassword)) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "SSLCommerz is not configured");
    }
    try {
      String url = validationApiUrl;
      RawHttpResponse response =
          webClientBuilder
              .build()
              .get()
              .uri(
                  url,
                  uriBuilder ->
                      uriBuilder
                          .queryParam("val_id", valId)
                          .queryParam("store_id", storeId)
                          .queryParam("store_passwd", storePassword)
                          .queryParam("v", "1")
                          .queryParam("format", "json")
                          .build())
              .exchangeToMono(
                  clientResponse ->
                      clientResponse
                          .bodyToMono(String.class)
                          .defaultIfEmpty("")
                          .map(
                              body ->
                                  new RawHttpResponse(clientResponse.statusCode().value(), body)))
              .block(Duration.ofMillis(validationTimeoutMs));
      if (response == null || response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
      }
      Map<String, Object> validated = parseValidationResponseBody(response.body());
      if (validated == null || validated.isEmpty()) {
        log.warn(
            "SSLCommerz validation API returned empty/unusable payload; val_id={}, statusCode={}",
            valId,
            response.statusCode());
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
      }
      return validated;
    } catch (Exception ex) {
      if (ex instanceof ResponseStatusException rse) {
        throw rse;
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback", ex);
    }
  }

  private void validateAgainstOrder(Order order, Map<String, Object> validated) {
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
      log.warn(
          "SSLCommerz validation failed: missing amount in validation API response; orderId={}, providerTxnId={}, validatedTranId={}",
          order.getId(),
          order.getProviderTxnId(),
          tranId);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid callback");
    }
    BigDecimal amount = new BigDecimal(amountRaw);

    boolean valid =
        transactionIdMatchesOrder(order, tranId)
            && currency != null
            && currency.equalsIgnoreCase(order.getCurrency())
            && order.getAmountBdt().subtract(amount).abs().compareTo(MAX_DIFF) < 0;
    if (!valid) {
      log.warn(
          "SSLCommerz validation mismatch: orderId={}, providerTxnId={}, validatedTranId={}, callbackOrderAmount={}, validatedAmount={}, callbackCurrency={}, validatedCurrency={}",
          order.getId(),
          order.getProviderTxnId(),
          tranId,
          order.getAmountBdt(),
          amount,
          order.getCurrency(),
          currency);
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

  private int parseRiskLevel(String value) {
    try {
      return value == null ? 0 : Integer.parseInt(value.trim());
    } catch (Exception ex) {
      return 0;
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseValidationResponseBody(String body) throws Exception {
    if (body == null || body.isBlank()) {
      return Map.of();
    }
    String trimmed = body.trim();
    if (trimmed.startsWith("[")) {
      List<Map<String, Object>> list =
          objectMapper.readValue(trimmed, new TypeReference<List<Map<String, Object>>>() {});
      if (list == null || list.isEmpty() || list.getFirst() == null) {
        return Map.of();
      }
      return list.getFirst();
    }
    return objectMapper.readValue(trimmed, MAP_TYPE);
  }

  private String computeSignature(List<String> fragments, String storePasswdValue) {
    List<String> sourceFragments = new ArrayList<>(fragments);
    sourceFragments.add("store_passwd=" + storePasswdValue);
    sourceFragments.sort(Comparator.naturalOrder());
    return md5Hex(String.join("&", sourceFragments));
  }

  private boolean equalsSignature(String computed, String provided) {
    return MessageDigest.isEqual(
        computed.toLowerCase().getBytes(StandardCharsets.UTF_8),
        provided.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
  }

  private boolean transactionIdMatchesOrder(Order order, String validatedTranId) {
    if (isBlank(validatedTranId)) {
      return false;
    }
    String callback = normalizeTxn(validatedTranId);
    String providerTxn = normalizeTxn(order.getProviderTxnId());
    if (!isBlank(providerTxn) && callback.equals(providerTxn)) {
      return true;
    }
    String orderDerivedTxn = normalizeTxn(order.getId().toString()).substring(0, 30);
    return callback.equals(orderDerivedTxn);
  }

  private String normalizeTxn(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("-", "").trim().toLowerCase();
  }

  @SuppressWarnings("java:S4790") // SSLCommerz verify_sign contract requires MD5 hashing.
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

  public record ValidationOutcome(String status, int riskLevel) {}
}
