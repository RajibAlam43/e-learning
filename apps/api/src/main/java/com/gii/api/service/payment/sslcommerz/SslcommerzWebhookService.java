package com.gii.api.service.payment.sslcommerz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.api.model.response.payment.WebhookAckResponse;
import com.gii.api.service.payment.callback.PaymentCallbackService;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PaymentEventType;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SslcommerzWebhookService {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final Set<String> SUCCESS = Set.of("VALID", "VALIDATED");
  private static final Set<String> FAILED = Set.of("FAILED", "EXPIRED", "UNATTEMPTED");
  private static final Set<String> CANCELLED = Set.of("CANCELLED", "CANCEL");

  private final PaymentEventRepository paymentEventRepository;
  private final OrderRepository orderRepository;
  private final PaymentCallbackService paymentCallbackService;
  private final SslcommerzCallbackValidationService sslcommerzCallbackValidationService;
  private final ObjectMapper objectMapper;
  @Value("${payments.sslcommerz.validate-on-webhook:true}")
  private boolean validateOnWebhook;

  public WebhookAckResponse handle(Map<String, String> headers, String payload) {
    Map<String, String> h = normalizeHeaders(headers);
    String providerEventId = firstNonBlank(h.get("x-event-id"), h.get("x-request-id"));
    if (providerEventId != null) {
      var existing =
          paymentEventRepository.findByProviderAndProviderEventId(OrderProvider.SSLCOMMERZ, providerEventId);
      if (existing.isPresent()) {
        return acknowledged("Webhook already received", existing.get().getId().toString());
      }
    }

    Map<String, Object> parsed = parsePayload(payload);
    Map<String, String> callbackParams = callbackParams(parsed, null);
    sslcommerzCallbackValidationService.validateWebhookSignature(callbackParams);
    String txnId = firstNonBlank(asString(parsed.get("tran_id")), h.get("x-transaction-id"), h.get("x-tran-id"));
    Optional<Order> orderOpt =
        txnId == null
            ? Optional.empty()
            : orderRepository.findByProviderAndProviderTxnId(OrderProvider.SSLCOMMERZ, txnId);

    PaymentEventStatus status = PaymentEventStatus.RECEIVED;
    if (orderOpt.isPresent()) {
      Order order = orderOpt.get();
      Map<String, String> params = callbackParams(parsed, txnId);
      String resolvedStatus = normalizeUpper(firstNonBlank(asString(parsed.get("status")), h.get("x-status")));
      int riskLevel = 0;
      if (validateOnWebhook) {
        SslcommerzCallbackValidationService.ValidationOutcome outcome =
            sslcommerzCallbackValidationService.validateIpnNotification(order, params);
        resolvedStatus = normalizeUpper(outcome.status());
        riskLevel = outcome.riskLevel();
      }

      if (SUCCESS.contains(resolvedStatus)) {
        if (riskLevel == 1) {
          PaymentEvent saved =
              paymentEventRepository.save(
                  PaymentEvent.builder()
                      .order(order)
                      .provider(OrderProvider.SSLCOMMERZ)
                      .eventType(PaymentEventType.SSLCOMMERZ_WEBHOOK_RISK_HOLD)
                      .providerEventId(providerEventId)
                      .rawPayloadJson(Map.of("headers", new HashMap<>(headers), "payload", payload))
                      .status(PaymentEventStatus.RECEIVED)
                      .processedAt(Instant.now())
                      .build());
          return acknowledged("Webhook received and held for risk verification", saved.getId().toString());
        }
        paymentCallbackService.successFromVerifiedWebhook(order.getId(), params);
        status = PaymentEventStatus.PROCESSED;
      } else if (FAILED.contains(resolvedStatus)) {
        paymentCallbackService.failed(order.getId(), params);
        status = PaymentEventStatus.PROCESSED;
      } else if (CANCELLED.contains(resolvedStatus)) {
        paymentCallbackService.cancelled(order.getId(), params);
        status = PaymentEventStatus.PROCESSED;
      }
    }

    PaymentEvent saved =
        paymentEventRepository.save(
            PaymentEvent.builder()
                .order(orderOpt.orElse(null))
                .provider(OrderProvider.SSLCOMMERZ)
                .eventType(PaymentEventType.SSLCOMMERZ_WEBHOOK)
                .providerEventId(providerEventId)
                .rawPayloadJson(Map.of("headers", new HashMap<>(headers), "payload", payload))
                .status(status)
                .processedAt(Instant.now())
                .build());
    return acknowledged("Webhook received", saved.getId().toString());
  }

  private Map<String, String> callbackParams(Map<String, Object> parsed, String txnId) {
    Map<String, String> params = new HashMap<>();
    for (Map.Entry<String, Object> e : parsed.entrySet()) {
      if (e.getValue() != null) params.put(e.getKey(), asString(e.getValue()));
    }
    if (txnId != null) params.put("tran_id", txnId);
    return params;
  }

  private Map<String, Object> parsePayload(String payload) {
    if (payload == null || payload.isBlank()) return Map.of();
    try {
      return objectMapper.readValue(payload, MAP_TYPE);
    } catch (Exception ignored) {
      Map<String, Object> form = new HashMap<>();
      for (String pair : payload.split("&")) {
        if (pair == null || pair.isBlank()) continue;
        int idx = pair.indexOf('=');
        String k = URLDecoder.decode(idx >= 0 ? pair.substring(0, idx) : pair, StandardCharsets.UTF_8);
        String v = URLDecoder.decode(idx >= 0 ? pair.substring(idx + 1) : "", StandardCharsets.UTF_8);
        form.put(k, v);
      }
      return form;
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

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
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
