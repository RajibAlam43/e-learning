package com.gii.api.service.payment.bkash;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.api.model.response.payment.WebhookAckResponse;
import com.gii.api.service.payment.callback.BkashCallbackService;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PaymentEventType;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BkashSnsWebhookService {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final Set<String> SUCCESS = Set.of("COMPLETED");
  private static final Set<String> FAILED = Set.of("FAILED");
  private static final Set<String> CANCELLED = Set.of("CANCELLED", "CANCELED");

  private final PaymentEventRepository paymentEventRepository;
  private final OrderRepository orderRepository;
  private final BkashCallbackService bkashCallbackService;
  private final ObjectMapper objectMapper;
  private final BkashSnsSignatureVerifier bkashSnsSignatureVerifier;

  public WebhookAckResponse handle(Map<String, String> headers, String payload) {
    Message message = parseSnsMessage(payload);
    verifySnsSignature(message);
    String messageType =
        normalizeSnsType(
            firstNonBlank(headerValue(headers, "x-amz-sns-message-type"), message.Type()));

    if ("subscriptionconfirmation".equals(messageType)) {
      confirmSubscription(message.SubscribeURL());
      return acknowledged("Subscription confirmed", message.MessageId());
    }
    if ("unsubscribeconfirmation".equals(messageType)) {
      return acknowledged("Unsubscribe confirmation received", message.MessageId());
    }
    if (!"notification".equals(messageType)) {
      return acknowledged("Unknown SNS message type", message.MessageId());
    }

    Map<String, Object> parsedNotification = parseNotificationMessage(message.Message());
    String providerEventId =
        firstNonBlank(message.MessageId(), headerValue(headers, "x-request-id"));
    if (providerEventId != null) {
      Optional<PaymentEvent> existing =
          paymentEventRepository.findByProviderAndProviderEventId(
              OrderProvider.BKASH, providerEventId);
      if (existing.isPresent()) {
        return acknowledged("Webhook already received", existing.get().getId().toString());
      }
    }

    String txnId =
        firstNonBlank(
            asString(parsedNotification.get("paymentID")),
            asString(parsedNotification.get("paymentId")),
            asString(parsedNotification.get("trxID")));
    Optional<Order> orderOpt =
        txnId == null
            ? Optional.empty()
            : orderRepository.findByProviderAndProviderTxnId(OrderProvider.BKASH, txnId);

    PaymentEventStatus eventStatus = PaymentEventStatus.RECEIVED;
    if (orderOpt.isPresent()) {
      String status =
          normalizeUpper(
              firstNonBlank(
                  asString(parsedNotification.get("transactionStatus")),
                  asString(parsedNotification.get("status"))));
      if (SUCCESS.contains(status)) {
        bkashCallbackService.successFromWebhook(orderOpt.get().getId(), callbackParams(txnId));
        eventStatus = PaymentEventStatus.PROCESSED;
      } else if (FAILED.contains(status)) {
        bkashCallbackService.failedFromWebhook(orderOpt.get().getId(), callbackParams(txnId));
        eventStatus = PaymentEventStatus.PROCESSED;
      } else if (CANCELLED.contains(status)) {
        bkashCallbackService.cancelledFromWebhook(orderOpt.get().getId(), callbackParams(txnId));
        eventStatus = PaymentEventStatus.PROCESSED;
      }
    }

    PaymentEvent saved =
        paymentEventRepository.save(
            PaymentEvent.builder()
                .order(orderOpt.orElse(null))
                .provider(OrderProvider.BKASH)
                .eventType(PaymentEventType.BKASH_WEBHOOK_SNS)
                .providerEventId(providerEventId)
                .rawPayloadJson(Map.of("headers", new HashMap<>(headers), "payload", payload))
                .status(eventStatus)
                .processedAt(Instant.now())
                .build());
    return acknowledged("Webhook received", saved.getId().toString());
  }

  private Message parseSnsMessage(String payload) {
    try {
      return objectMapper.readValue(payload, Message.class);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook payload", ex);
    }
  }

  private void verifySnsSignature(Message message) {
    if (!bkashSnsSignatureVerifier.isValid(message)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
    }
  }

  private void confirmSubscription(String subscribeUrl) {
    if (subscribeUrl == null || subscribeUrl.isBlank()) {
      return;
    }
    try (Scanner scanner = new Scanner(new URL(subscribeUrl).openStream())) {
      while (scanner.hasNextLine()) {
        scanner.nextLine();
      }
    } catch (Exception ex) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Failed to confirm subscription", ex);
    }
  }

  private Map<String, Object> parseNotificationMessage(String messageBody) {
    if (messageBody == null || messageBody.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(messageBody, MAP_TYPE);
    } catch (Exception ex) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid notification message", ex);
    }
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

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private String normalizeUpper(String value) {
    return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizeSnsType(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
  }

  private String headerValue(Map<String, String> headers, String targetKey) {
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(targetKey)) {
        return entry.getValue();
      }
    }
    return null;
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
