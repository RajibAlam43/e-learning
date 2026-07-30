package com.gii.api.service.payment.sslcommerz;

import com.gii.api.model.response.payment.WebhookAckResponse;
import com.gii.api.service.payment.callback.SslcommerzCallbackService;
import com.gii.api.service.util.SslcommerzValidationJobPublisherService;
import com.gii.common.dto.SslcommerzValidationJobMessage;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PaymentEventType;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SslcommerzWebhookService {

  private static final Set<String> SUCCESS = Set.of("VALID", "VALIDATED");
  private static final Set<String> FAILED = Set.of("FAILED", "EXPIRED", "UNATTEMPTED");
  private static final Set<String> CANCELLED = Set.of("CANCELLED", "CANCEL");

  private final PaymentEventRepository paymentEventRepository;
  private final OrderRepository orderRepository;
  private final SslcommerzCallbackService sslcommerzCallbackService;
  private final SslcommerzCallbackValidationService sslcommerzCallbackValidationService;
  private final SslcommerzValidationJobPublisherService validationJobPublisherService;

  @Value("${payments.sslcommerz.validate-on-webhook:true}")
  private boolean validateOnWebhook;

  public WebhookAckResponse handle(Map<String, String> headers, Map<String, String> params) {
    Map<String, String> h = normalizeHeaders(headers);
    String providerEventId = firstNonBlank(h.get("x-event-id"), h.get("x-request-id"));
    if (providerEventId != null) {
      var existing =
          paymentEventRepository.findByProviderAndProviderEventId(
              OrderProvider.SSLCOMMERZ, providerEventId);
      if (existing.isPresent()) {
        return acknowledged("Webhook already received", existing.get().getId().toString());
      }
    }

    sslcommerzCallbackValidationService.validateWebhookSignature(params);
    String txnId =
        firstNonBlank(params.get("tran_id"), h.get("x-transaction-id"), h.get("x-tran-id"));
    Optional<Order> orderOpt =
        txnId == null
            ? Optional.empty()
            : orderRepository.findByProviderAndProviderTxnId(OrderProvider.SSLCOMMERZ, txnId);

    PaymentEventStatus status = PaymentEventStatus.RECEIVED;
    if (orderOpt.isPresent()) {
      Order order = orderOpt.get();
      Map<String, String> callbackParams = new HashMap<>(params);
      callbackParams.put("tran_id", txnId);
      callbackParams.put("_verified_webhook", "true");
      String resolvedStatus =
          normalizeUpper(firstNonBlank(params.get("status"), h.get("x-status")));
      if (order.getStatus() == OrderStatus.PAID && SUCCESS.contains(resolvedStatus)) {
        PaymentEvent saved =
            paymentEventRepository.save(
                PaymentEvent.builder()
                    .order(order)
                    .provider(OrderProvider.SSLCOMMERZ)
                    .eventType(PaymentEventType.SSLCOMMERZ_WEBHOOK)
                    .providerEventId(providerEventId)
                    .rawPayloadJson(
                        Map.of("headers", new HashMap<>(headers), "payload", new HashMap<>(params)))
                    .status(PaymentEventStatus.PROCESSED)
                    .processedAt(Instant.now())
                    .build());
        return acknowledged("Webhook received for already-paid order", saved.getId().toString());
      }
      int riskLevel = 0;
      if (validateOnWebhook && SUCCESS.contains(resolvedStatus)) {
        try {
          SslcommerzCallbackValidationService.ValidationOutcome outcome =
              sslcommerzCallbackValidationService.validateIpnNotification(order, callbackParams);
          resolvedStatus = normalizeUpper(outcome.status());
          riskLevel = outcome.riskLevel();
        } catch (ResponseStatusException ex) {
          try {
            validationJobPublisherService.publish(
                SslcommerzValidationJobMessage.builder()
                    .orderId(order.getId())
                    .providerTxnId(txnId)
                    .valId(callbackParams.get("val_id"))
                    .source("WEBHOOK")
                    .attempt(1)
                    .maxAttempts(6)
                    .createdAt(Instant.now())
                    .build());
          } catch (Exception publishEx) {
            log.error(
                "Failed to queue SSLCommerz webhook validation job; orderId={}, tran_id={}",
                order.getId(),
                txnId,
                publishEx);
          }
          log.warn(
              "Queued SSLCommerz validation job after webhook validation failure; orderId={}, tran_id={}, reason={}",
              order.getId(),
              txnId,
              ex.getReason());
          PaymentEvent saved =
              paymentEventRepository.save(
                  PaymentEvent.builder()
                      .order(order)
                      .provider(OrderProvider.SSLCOMMERZ)
                      .eventType(PaymentEventType.SSLCOMMERZ_WEBHOOK)
                      .providerEventId(providerEventId)
                      .rawPayloadJson(
                          Map.of(
                              "headers", new HashMap<>(headers), "payload", new HashMap<>(params)))
                      .status(PaymentEventStatus.RECEIVED)
                      .processedAt(Instant.now())
                      .build());
          return acknowledged(
              "Webhook received and queued for validation", saved.getId().toString());
        }
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
                      .rawPayloadJson(
                          Map.of(
                              "headers", new HashMap<>(headers), "payload", new HashMap<>(params)))
                      .status(PaymentEventStatus.RECEIVED)
                      .processedAt(Instant.now())
                      .build());
          return acknowledged(
              "Webhook received and held for risk verification", saved.getId().toString());
        }
        sslcommerzCallbackService.successFromWebhook(order.getId(), callbackParams);
        status = PaymentEventStatus.PROCESSED;
      } else if (FAILED.contains(resolvedStatus)) {
        sslcommerzCallbackService.failedFromWebhook(order.getId(), callbackParams);
        status = PaymentEventStatus.PROCESSED;
      } else if (CANCELLED.contains(resolvedStatus)) {
        sslcommerzCallbackService.cancelledFromWebhook(order.getId(), callbackParams);
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
                .rawPayloadJson(
                    Map.of("headers", new HashMap<>(headers), "payload", new HashMap<>(params)))
                .status(status)
                .processedAt(Instant.now())
                .build());
    return acknowledged("Webhook received", saved.getId().toString());
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

  private WebhookAckResponse acknowledged(String message, String webhookId) {
    return WebhookAckResponse.builder()
        .acknowledged(Boolean.TRUE)
        .message(message)
        .webhookId(webhookId)
        .processingDelayMs(0L)
        .build();
  }
}
