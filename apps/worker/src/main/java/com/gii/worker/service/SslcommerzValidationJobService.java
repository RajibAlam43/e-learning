package com.gii.worker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.dto.SslcommerzValidationJobMessage;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PaymentEventType;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentAttemptRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import com.gii.common.service.payment.PaidOrderEnrollmentService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class SslcommerzValidationJobService {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<Map<String, Object>>> LIST_TYPE =
      new TypeReference<>() {};
  private static final BigDecimal MAX_DIFF = new BigDecimal("0.1");

  @Qualifier("jacksonObjectMapper")
  private final ObjectMapper objectMapper;

  private final WebClient.Builder webClientBuilder;
  private final SqsAsyncClient sqsClient;
  private final OrderRepository orderRepository;
  private final PaymentEventRepository paymentEventRepository;
  private final PaymentAttemptRepository paymentAttemptRepository;
  private final PaidOrderEnrollmentService paidOrderEnrollmentService;
  private final Map<String, String> queueUrlCache = new ConcurrentHashMap<>();

  @Value("${payments.sslcommerz.validation-api-url}")
  private String validationApiUrl;

  @Value("${payments.sslcommerz.store-id}")
  private String storeId;

  @Value("${payments.sslcommerz.store-password}")
  private String storePassword;

  @Value("${payments.sslcommerz.validation-timeout-ms}")
  private long validationTimeoutMs;

  @Value("${payments.sslcommerz.validation.jobs.queue}")
  private String validationQueue;

  @Transactional
  public void process(SslcommerzValidationJobMessage job) {
    Order order = resolveOrder(job);
    if (order == null) {
      log.warn(
          "SSLCommerz validation job skipped: order not found; orderId={}, providerTxnId={}, source={}, attempt={}",
          job.orderId(),
          job.providerTxnId(),
          job.source(),
          job.attempt());
      return;
    }
    if (order.getStatus() == OrderStatus.PAID) {
      paidOrderEnrollmentService.grant(order.getId());
      return;
    }
    if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
      log.info(
          "SSLCommerz validation job skipped for terminal order state; orderId={}, status={}, valId={}, attempt={}",
          order.getId(),
          order.getStatus(),
          job.valId(),
          job.attempt());
      return;
    }

    try {
      Map<String, Object> validated = validateByValId(job.valId());
      validateAgainstOrder(order, validated, job.providerTxnId());
      String status = normalize(asString(validated.get("status")));
      if ("VALID".equals(status) || "VALIDATED".equals(status)) {
        int riskLevel = parseRiskLevel(asString(validated.get("risk_level")));
        if (riskLevel == 1) {
          recordRiskHoldEvent(order, job);
          log.warn(
              "SSLCommerz worker validation held for manual risk review; orderId={}, tranId={}, valId={}, attempt={}/{}",
              order.getId(),
              job.providerTxnId(),
              job.valId(),
              job.attempt(),
              job.maxAttempts());
          return;
        }
        markPaid(order);
        paidOrderEnrollmentService.grant(order.getId());
        recordEvent(order, job, PaymentEventStatus.PROCESSED);
        return;
      }
      throw new IllegalStateException("Validation API returned non-success status: " + status);
    } catch (Exception ex) {
      int nextAttempt = job.attempt() + 1;
      if (nextAttempt <= job.maxAttempts()) {
        requeue(job, nextAttempt);
        log.warn(
            "Requeued SSLCommerz validation job; orderId={}, tranId={}, attempt={}/{}, delaySeconds={}, reason={}",
            order.getId(),
            job.providerTxnId(),
            nextAttempt,
            job.maxAttempts(),
            delaySecondsForAttempt(nextAttempt),
            ex.getMessage());
        return;
      }
      transitionFailed(order);
      recordEvent(order, job, PaymentEventStatus.FAILED);
      log.error(
          "SSLCommerz validation failed after max attempts; orderId={}, tranId={}, attempts={}, valId={}",
          order.getId(),
          job.providerTxnId(),
          job.attempt(),
          job.valId(),
          ex);
    }
  }

  private Order resolveOrder(SslcommerzValidationJobMessage job) {
    if (job.orderId() != null) {
      Optional<Order> byId = orderRepository.findById(job.orderId());
      if (byId.isPresent()) {
        return byId.get();
      }
    }
    if (job.providerTxnId() == null || job.providerTxnId().isBlank()) {
      return null;
    }
    return paymentAttemptRepository
        .findOrderByProviderAndProviderTxnId(OrderProvider.SSLCOMMERZ, job.providerTxnId())
        .or(
            () ->
                orderRepository.findByProviderAndProviderTxnId(
                    OrderProvider.SSLCOMMERZ, job.providerTxnId()))
        .orElse(null);
  }

  private Map<String, Object> validateByValId(String valId) throws Exception {
    if (valId == null || valId.isBlank()) {
      throw new IllegalStateException("Missing val_id");
    }
    RawHttpResponse response =
        webClientBuilder
            .build()
            .get()
            .uri(
                validationApiUrl,
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
                            body -> new RawHttpResponse(clientResponse.statusCode().value(), body)))
            .block(Duration.ofMillis(validationTimeoutMs));
    if (response == null || response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException("Validation API call failed");
    }
    Map<String, Object> parsed = parseValidationResponseBody(response.body());
    if (parsed.isEmpty()) {
      throw new IllegalStateException("Validation API returned empty payload");
    }
    return parsed;
  }

  private Map<String, Object> parseValidationResponseBody(String body) throws Exception {
    if (body == null || body.isBlank()) {
      return Map.of();
    }
    String trimmed = body.trim();
    if (trimmed.startsWith("[")) {
      List<Map<String, Object>> list = objectMapper.readValue(trimmed, LIST_TYPE);
      return list == null || list.isEmpty() || list.getFirst() == null ? Map.of() : list.getFirst();
    }
    return objectMapper.readValue(trimmed, MAP_TYPE);
  }

  private void validateAgainstOrder(
      Order order, Map<String, Object> validated, String providerTxnId) {
    String tranId = normalizeTxn(asString(validated.get("tran_id")));
    String expectedTxn = normalizeTxn(providerTxnId);
    if (!tranId.equals(expectedTxn)) {
      throw new IllegalStateException("Transaction mismatch");
    }

    String currency = asString(validated.get("currency_type"));
    if (currency == null || currency.isBlank()) {
      currency = asString(validated.get("currency"));
    }
    if (currency == null || !currency.equalsIgnoreCase(order.getCurrency())) {
      throw new IllegalStateException("Currency mismatch");
    }

    String amountRaw = asString(validated.get("amount"));
    if (amountRaw == null || amountRaw.isBlank()) {
      amountRaw = asString(validated.get("currency_amount"));
    }
    if (amountRaw == null || amountRaw.isBlank()) {
      throw new IllegalStateException("Missing amount");
    }
    BigDecimal amount = new BigDecimal(amountRaw);
    if (order.getAmountBdt().subtract(amount).abs().compareTo(MAX_DIFF) >= 0) {
      throw new IllegalStateException("Amount mismatch");
    }
  }

  private void requeue(SslcommerzValidationJobMessage job, int nextAttempt) {
    SslcommerzValidationJobMessage next =
        SslcommerzValidationJobMessage.builder()
            .orderId(job.orderId())
            .providerTxnId(job.providerTxnId())
            .valId(job.valId())
            .source(job.source())
            .attempt(nextAttempt)
            .maxAttempts(job.maxAttempts())
            .createdAt(job.createdAt())
            .build();
    try {
      String payload = objectMapper.writeValueAsString(next);
      String queueUrl =
          queueUrlCache.computeIfAbsent(
              validationQueue,
              name ->
                  sqsClient
                      .getQueueUrl(GetQueueUrlRequest.builder().queueName(name).build())
                      .join()
                      .queueUrl());
      sqsClient
          .sendMessage(
              SendMessageRequest.builder()
                  .queueUrl(queueUrl)
                  .messageBody(payload)
                  .delaySeconds(delaySecondsForAttempt(nextAttempt))
                  .build())
          .join();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to requeue SSLCommerz validation job", ex);
    }
  }

  private int delaySecondsForAttempt(int nextAttempt) {
    return switch (nextAttempt) {
      case 2 -> 60;
      case 3 -> 300;
      case 4 -> 900;
      default -> 1800;
    };
  }

  private void recordEvent(
      Order order, SslcommerzValidationJobMessage job, PaymentEventStatus status) {
    paymentEventRepository.save(
        PaymentEvent.builder()
            .order(order)
            .provider(OrderProvider.SSLCOMMERZ)
            .eventType(PaymentEventType.SSLCOMMERZ_WEBHOOK)
            .providerEventId(job.providerTxnId())
            .rawPayloadJson(
                Map.of(
                    "source", job.source(),
                    "val_id", job.valId(),
                    "attempt", String.valueOf(job.attempt()),
                    "max_attempts", String.valueOf(job.maxAttempts())))
            .status(status)
            .processedAt(Instant.now())
            .build());
  }

  private void recordRiskHoldEvent(Order order, SslcommerzValidationJobMessage job) {
    paymentEventRepository.save(
        PaymentEvent.builder()
            .order(order)
            .provider(OrderProvider.SSLCOMMERZ)
            .eventType(PaymentEventType.SSLCOMMERZ_WEBHOOK_RISK_HOLD)
            .providerEventId(job.providerTxnId())
            .rawPayloadJson(
                Map.of(
                    "source", job.source(),
                    "val_id", job.valId(),
                    "attempt", String.valueOf(job.attempt()),
                    "max_attempts", String.valueOf(job.maxAttempts())))
            .status(PaymentEventStatus.RECEIVED)
            .processedAt(Instant.now())
            .build());
  }

  private void markPaid(Order order) {
    order.setStatus(OrderStatus.PAID);
    if (order.getPaidAt() == null) {
      order.setPaidAt(Instant.now());
    }
    orderRepository.save(order);
  }

  private void transitionFailed(Order order) {
    if (order.getStatus() == OrderStatus.PENDING) {
      order.setStatus(OrderStatus.FAILED);
      orderRepository.save(order);
    }
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase();
  }

  private String normalizeTxn(String value) {
    return value == null ? "" : value.replace("-", "").trim().toLowerCase();
  }

  private int parseRiskLevel(String value) {
    try {
      return value == null ? 0 : Integer.parseInt(value.trim());
    } catch (Exception ex) {
      return 0;
    }
  }

  private record RawHttpResponse(int statusCode, String body) {}
}
