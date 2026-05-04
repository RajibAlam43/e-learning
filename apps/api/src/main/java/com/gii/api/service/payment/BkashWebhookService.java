package com.gii.api.service.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.api.model.response.payment.WebhookAckResponse;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
class BkashWebhookService {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final Set<String> BKASH_SNS_ALLOWED_TYPES =
      Set.of("notification", "subscriptionconfirmation", "unsubscribeconfirmation");

  private final PaymentEventRepository paymentEventRepository;
  private final OrderRepository orderRepository;
  private final PaymentCallbackService paymentCallbackService;
  private final ObjectMapper objectMapper;

  @Value("${payments.bkash.webhook-secret}")
  private String bkashWebhookSecret;

  WebhookAckResponse handle(Map<String, String> headers, String payload) {
    Map<String, String> h = normalizeHeaders(headers);
    verifySignature(h, payload);

    String providerEventId =
        firstNonBlank(h.get("x-event-id"), h.get("x-transaction-id"), h.get("x-request-id"));
    if (providerEventId != null) {
      var existing =
          paymentEventRepository.findByProviderAndProviderEventId(OrderProvider.BKASH, providerEventId);
      if (existing.isPresent()) {
        return acknowledged("Webhook already received", existing.get().getId().toString());
      }
    }

    Map<String, Object> parsed = parsePayload(payload);
    Map<String, Object> notification = notificationPayload(parsed);
    String txnId =
        firstNonBlank(
            asString(notification.get("paymentID")),
            asString(notification.get("paymentId")),
            asString(notification.get("trxID")),
            h.get("x-payment-id"),
            h.get("x-transaction-id"),
            h.get("x-tran-id"));
    Optional<Order> orderOpt =
        txnId == null
            ? Optional.empty()
            : orderRepository.findByProviderAndProviderTxnId(OrderProvider.BKASH, txnId);

    PaymentEventStatus status = PaymentEventStatus.RECEIVED;
    if (orderOpt.isPresent()) {
      String s =
          normalizeUpper(
              firstNonBlank(
                  asString(notification.get("transactionStatus")),
                  asString(notification.get("status")),
                  h.get("x-payment-status"),
                  h.get("x-status")));
      if ("COMPLETED".equals(s)) {
        paymentCallbackService.successFromVerifiedWebhook(orderOpt.get().getId(), callbackParams(txnId));
        status = PaymentEventStatus.PROCESSED;
      } else if ("FAILED".equals(s)) {
        paymentCallbackService.failed(orderOpt.get().getId(), callbackParams(txnId));
        status = PaymentEventStatus.PROCESSED;
      } else if ("CANCELLED".equals(s) || "CANCELED".equals(s)) {
        paymentCallbackService.cancelled(orderOpt.get().getId(), callbackParams(txnId));
        status = PaymentEventStatus.PROCESSED;
      }
    }

    PaymentEvent saved =
        paymentEventRepository.save(
            PaymentEvent.builder()
                .order(orderOpt.orElse(null))
                .provider(OrderProvider.BKASH)
                .eventType("bkash_webhook")
                .providerEventId(providerEventId)
                .rawPayloadJson(Map.of("headers", new HashMap<>(headers), "payload", payload))
                .status(status)
                .processedAt(Instant.now())
                .build());
    return acknowledged("Webhook received", saved.getId().toString());
  }

  private void verifySignature(Map<String, String> headers, String payload) {
    require(bkashWebhookSecret != null && !bkashWebhookSecret.isBlank(), HttpStatus.SERVICE_UNAVAILABLE);
    String provided =
        firstNonBlank(
            headers.get("x-signature"),
            headers.get("x-signature-sha256"),
            headers.get("x-webhook-signature"),
            headers.get("x-bkash-signature"),
            headers.get("signature"));
    require(provided != null && !provided.isBlank(), HttpStatus.BAD_REQUEST);

    String normalized = provided.trim();
    if (normalized.regionMatches(true, 0, "sha256=", 0, 7)) normalized = normalized.substring(7);
    String expectedHex = hmacHex(payload, bkashWebhookSecret);
    String expectedB64 = hmacBase64(payload, bkashWebhookSecret);
    boolean ok =
        MessageDigest.isEqual(normalized.getBytes(StandardCharsets.UTF_8), expectedHex.getBytes(StandardCharsets.UTF_8))
            || MessageDigest.isEqual(
                normalized.getBytes(StandardCharsets.UTF_8), expectedB64.getBytes(StandardCharsets.UTF_8));
    require(ok, HttpStatus.BAD_REQUEST);
  }

  private Map<String, String> callbackParams(String txnId) {
    Map<String, String> params = new HashMap<>();
    if (txnId != null) {
      params.put("tran_id", txnId);
      params.put("payment_id", txnId);
      params.put("paymentID", txnId);
    }
    return params;
  }

  private Map<String, Object> parsePayload(String payload) {
    if (payload == null || payload.isBlank()) return Map.of();
    try {
      return objectMapper.readValue(payload, MAP_TYPE);
    } catch (Exception ignored) {
      return Map.of();
    }
  }

  private Map<String, Object> notificationPayload(Map<String, Object> parsed) {
    String snsType = normalizeLower(firstNonBlank(asString(parsed.get("Type")), asString(parsed.get("type"))));
    if (snsType != null && BKASH_SNS_ALLOWED_TYPES.contains(snsType)) {
      Object message = parsed.get("Message");
      if (message instanceof String text && !text.isBlank()) {
        try {
          return objectMapper.readValue(text, MAP_TYPE);
        } catch (Exception ignored) {
          return parsed;
        }
      }
    }
    return parsed;
  }

  private String hmacHex(String payload, String secret) {
    byte[] digest = hmac(payload, secret);
    StringBuilder sb = new StringBuilder(digest.length * 2);
    for (byte b : digest) sb.append(String.format("%02x", b));
    return sb.toString();
  }

  private String hmacBase64(String payload, String secret) {
    return Base64.getEncoder().encodeToString(hmac(payload, secret));
  }

  private byte[] hmac(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Webhook verification failed");
    }
  }

  private Map<String, String> normalizeHeaders(Map<String, String> headers) {
    Map<String, String> normalized = new HashMap<>();
    headers.forEach((k, v) -> normalized.put(k.toLowerCase(Locale.ROOT), v));
    return normalized;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) if (value != null && !value.isBlank()) return value;
    return null;
  }

  private String normalizeUpper(String value) {
    return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizeLower(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private void require(boolean condition, HttpStatus status) {
    if (!condition) {
      throw new ResponseStatusException(status, "Invalid webhook");
    }
  }

  private WebhookAckResponse acknowledged(String message, String webhookId) {
    return WebhookAckResponse.builder()
        .acknowledged(Boolean.TRUE)
        .message(message)
        .webhookId(webhookId)
        .processingDelayMs(0L)
        .build();
  }
}
