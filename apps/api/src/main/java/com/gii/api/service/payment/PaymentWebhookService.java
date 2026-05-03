package com.gii.api.service.payment;

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
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentWebhookService {

  private final PaymentEventRepository paymentEventRepository;
  private final OrderRepository orderRepository;
  private final PaymentCallbackService paymentCallbackService;

  @Value("${payments.sslcommerz.webhook-secret:}")
  private String sslcommerzWebhookSecret;

  @Value("${payments.bkash.webhook-secret:}")
  private String bkashWebhookSecret;

  public WebhookAckResponse sslcommerz(Map<String, String> headers, String payload) {
    verifySignature(OrderProvider.SSLCOMMERZ, headers, payload);
    return acknowledge(OrderProvider.SSLCOMMERZ, "sslcommerz_webhook", headers, payload);
  }

  public WebhookAckResponse bkash(Map<String, String> headers, String payload) {
    verifySignature(OrderProvider.BKASH, headers, payload);
    return acknowledge(OrderProvider.BKASH, "bkash_webhook", headers, payload);
  }

  public WebhookAckResponse nagad(Map<String, String> headers, String payload) {
    // Current enum/schema does not support NAGAD provider yet.
    return WebhookAckResponse.builder()
        .acknowledged(Boolean.FALSE)
        .message("Nagad provider is not enabled in current configuration")
        .webhookId(null)
        .processingDelayMs(0L)
        .build();
  }

  private WebhookAckResponse acknowledge(
      OrderProvider provider, String eventType, Map<String, String> headers, String payload) {
    String providerEventId =
        firstNonBlank(
            headers.get("x-event-id"),
            headers.get("x-transaction-id"),
            headers.get("x-request-id"));
    if (providerEventId != null) {
      var existing =
          paymentEventRepository.findByProviderAndProviderEventId(provider, providerEventId);
      if (existing.isPresent()) {
        return WebhookAckResponse.builder()
            .acknowledged(Boolean.TRUE)
            .message("Webhook already received")
            .webhookId(existing.get().getId().toString())
            .processingDelayMs(0L)
            .build();
      }
    }
    Optional<Order> orderOpt = findOrderFromHeaders(provider, headers);

    Map<String, Object> rawPayload = new HashMap<>();
    rawPayload.put("headers", new HashMap<>(headers));
    rawPayload.put("payload", payload);

    PaymentEventStatus processingStatus = PaymentEventStatus.RECEIVED;
    if (orderOpt.isPresent()) {
      PaymentWebhookEvent webhookEvent = inferWebhookEvent(headers, payload);
      if (webhookEvent == PaymentWebhookEvent.SUCCESS) {
        paymentCallbackService.success(orderOpt.get().getId(), callbackParamsFromHeaders(headers));
        paymentCallbackService.grantEnrollmentsForPaidOrder(orderOpt.get().getId());
        processingStatus = PaymentEventStatus.PROCESSED;
      } else if (webhookEvent == PaymentWebhookEvent.FAILED) {
        paymentCallbackService.failed(orderOpt.get().getId(), callbackParamsFromHeaders(headers));
        processingStatus = PaymentEventStatus.PROCESSED;
      } else if (webhookEvent == PaymentWebhookEvent.CANCELLED) {
        paymentCallbackService.cancelled(
            orderOpt.get().getId(), callbackParamsFromHeaders(headers));
        processingStatus = PaymentEventStatus.PROCESSED;
      }
    }

    PaymentEvent event =
        PaymentEvent.builder()
            .order(orderOpt.orElse(null))
            .provider(provider)
            .eventType(eventType)
            .providerEventId(providerEventId)
            .rawPayloadJson(rawPayload)
            .status(processingStatus)
            .processedAt(Instant.now())
            .build();
    PaymentEvent savedEvent = paymentEventRepository.save(event);

    return WebhookAckResponse.builder()
        .acknowledged(Boolean.TRUE)
        .message("Webhook received")
        .webhookId(savedEvent.getId().toString())
        .processingDelayMs(0L)
        .build();
  }

  private void verifySignature(
      OrderProvider provider, Map<String, String> headers, String payload) {
    Map<String, String> normalizedHeaders = normalizeHeaders(headers);
    String secret = getWebhookSecret(provider);
    if (secret == null || secret.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Webhook secret is not configured");
    }

    String providedSignature = extractSignature(normalizedHeaders);
    if (providedSignature == null || providedSignature.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing webhook signature");
    }

    String expectedHex = hmacSha256Hex(payload, secret);
    String expectedBase64 = hmacSha256Base64(payload, secret);
    String normalizedProvidedSignature = normalizeSignature(providedSignature);

    boolean matchesHex = constantTimeEquals(normalizedProvidedSignature, expectedHex);
    boolean matchesBase64 = constantTimeEquals(normalizedProvidedSignature, expectedBase64);
    if (!matchesHex && !matchesBase64) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
    }
  }

  private Map<String, String> normalizeHeaders(Map<String, String> headers) {
    Map<String, String> normalized = new HashMap<>();
    headers.forEach((key, value) -> normalized.put(key.toLowerCase(), value));
    return normalized;
  }

  private String extractSignature(Map<String, String> headers) {
    return firstNonBlank(
        headers.get("x-signature"),
        headers.get("x-signature-sha256"),
        headers.get("x-webhook-signature"),
        headers.get("x-bkash-signature"),
        headers.get("signature"));
  }

  private String getWebhookSecret(OrderProvider provider) {
    return switch (provider) {
      case SSLCOMMERZ -> sslcommerzWebhookSecret;
      case BKASH -> bkashWebhookSecret;
      default -> null;
    };
  }

  private String hmacSha256Hex(String payload, String secret) {
    byte[] digest = hmacSha256(payload, secret);
    StringBuilder sb = new StringBuilder(digest.length * 2);
    for (byte b : digest) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private String hmacSha256Base64(String payload, String secret) {
    byte[] digest = hmacSha256(payload, secret);
    return Base64.getEncoder().encodeToString(digest);
  }

  private byte[] hmacSha256(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec keySpec =
          new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(keySpec);
      return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    } catch (Exception ex) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify webhook signature");
    }
  }

  private String normalizeSignature(String signature) {
    String normalized = signature.trim();
    if (normalized.regionMatches(true, 0, "sha256=", 0, 7)) {
      normalized = normalized.substring(7);
    }
    return normalized;
  }

  private boolean constantTimeEquals(String provided, String expected) {
    byte[] left = provided.getBytes(StandardCharsets.UTF_8);
    byte[] right = expected.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(left, right);
  }

  private java.util.Optional<Order> findOrderFromHeaders(
      OrderProvider provider, Map<String, String> headers) {
    String txnId =
        firstNonBlank(
            headers.get("x-transaction-id"), headers.get("x-tran-id"), headers.get("x-payment-id"));
    if (txnId == null) {
      return java.util.Optional.empty();
    }
    return orderRepository.findByProviderAndProviderTxnId(provider, txnId);
  }

  private Map<String, String> callbackParamsFromHeaders(Map<String, String> headers) {
    Map<String, String> params = new HashMap<>();
    String txnId =
        firstNonBlank(
            headers.get("x-transaction-id"), headers.get("x-tran-id"), headers.get("x-payment-id"));
    if (txnId != null) {
      params.put("tran_id", txnId);
    }
    return params;
  }

  private PaymentWebhookEvent inferWebhookEvent(Map<String, String> headers, String payload) {
    String hint =
        firstNonBlank(
            headers.get("x-event-type"),
            headers.get("x-payment-status"),
            headers.get("x-status"),
            headers.get("event"),
            payload);
    if (hint == null) {
      return PaymentWebhookEvent.UNKNOWN;
    }
    String normalized = hint.toLowerCase();
    if (normalized.contains("fail") || normalized.contains("declin") || normalized.contains("error")) {
      return PaymentWebhookEvent.FAILED;
    }
    if (normalized.contains("cancel")) {
      return PaymentWebhookEvent.CANCELLED;
    }
    if (normalized.contains("success")
        || normalized.contains("paid")
        || normalized.contains("complete")
        || normalized.contains("\"payment\"")) {
      return PaymentWebhookEvent.SUCCESS;
    }
    return PaymentWebhookEvent.UNKNOWN;
  }

  private enum PaymentWebhookEvent {
    SUCCESS,
    FAILED,
    CANCELLED,
    UNKNOWN
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }
}
