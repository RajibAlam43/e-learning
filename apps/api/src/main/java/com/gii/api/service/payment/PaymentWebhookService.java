package com.gii.api.service.payment;

import com.gii.api.model.response.payment.WebhookAckResponse;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentWebhookService {

  private final PaymentEventRepository paymentEventRepository;
  private final OrderRepository orderRepository;

  public WebhookAckResponse sslcommerz(Map<String, String> headers, String payload) {
    return acknowledge(OrderProvider.SSLCOMMERZ, "sslcommerz_webhook", headers, payload);
  }

  public WebhookAckResponse bkash(Map<String, String> headers, String payload) {
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
    String providerEventId = firstNonBlank(
        headers.get("x-event-id"), headers.get("x-transaction-id"), headers.get("x-request-id"));
    Order order = findOrderFromHeaders(provider, headers).orElse(null);

    Map<String, Object> rawPayload = new HashMap<>();
    rawPayload.put("headers", new HashMap<>(headers));
    rawPayload.put("payload", payload);

    PaymentEvent event =
        PaymentEvent.builder()
            .order(order)
            .provider(provider)
            .eventType(eventType)
            .providerEventId(providerEventId)
            .rawPayloadJson(rawPayload)
            .status(PaymentEventStatus.RECEIVED)
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

  private java.util.Optional<Order> findOrderFromHeaders(
      OrderProvider provider, Map<String, String> headers) {
    String txnId = firstNonBlank(
        headers.get("x-transaction-id"), headers.get("x-tran-id"), headers.get("x-payment-id"));
    if (txnId == null) {
      return java.util.Optional.empty();
    }
    return orderRepository.findByProviderAndProviderTxnId(provider, txnId);
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
