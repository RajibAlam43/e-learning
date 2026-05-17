package com.gii.worker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.dto.SslcommerzValidationJobMessage;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PaymentEventType;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
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
  private static final TypeReference<List<Map<String, Object>>> LIST_TYPE = new TypeReference<>() {};
  private static final BigDecimal MAX_DIFF = new BigDecimal("0.1");

  private final ObjectMapper objectMapper;
  private final WebClient.Builder webClientBuilder;
  private final SqsAsyncClient sqsClient;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CollectionEnrollmentRepository collectionEnrollmentRepository;
  private final CollectionCourseRepository collectionCourseRepository;
  private final PaymentEventRepository paymentEventRepository;
  private final Map<String, String> queueUrlCache = new ConcurrentHashMap<>();

  @Value("${payments.sslcommerz.validation-api-url}")
  private String validationApiUrl;

  @Value("${payments.sslcommerz.store-id}")
  private String storeId;

  @Value("${payments.sslcommerz.store-password}")
  private String storePassword;

  @Value("${payments.sslcommerz.validation-timeout-ms}")
  private long validationTimeoutMs;

  @Value("${payments.sslcommerz.validation.jobs.queue:gii-stage-sslcommerz-validation-queue}")
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
      return;
    }
    if (order.getStatus() == OrderStatus.CANCELLED
        || order.getStatus() == OrderStatus.REFUNDED) {
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
      validateAgainstOrder(order, validated);
      String status = normalize(asString(validated.get("status")));
      if ("VALID".equals(status) || "VALIDATED".equals(status)) {
        markPaid(order);
        grantEnrollmentsForPaidOrder(order.getId());
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
    return orderRepository
        .findByProviderAndProviderTxnId(OrderProvider.SSLCOMMERZ, job.providerTxnId())
        .orElse(null);
  }

  private Map<String, Object> validateByValId(String valId) throws Exception {
    if (valId == null || valId.isBlank()) {
      throw new IllegalStateException("Missing val_id");
    }
    String url =
        validationApiUrl
            + "?val_id="
            + URLEncoder.encode(valId, StandardCharsets.UTF_8)
            + "&store_id="
            + URLEncoder.encode(storeId, StandardCharsets.UTF_8)
            + "&store_passwd="
            + URLEncoder.encode(storePassword, StandardCharsets.UTF_8)
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
      throw new IllegalStateException("Validation API call failed");
    }
    log.info(
        "SSLCommerz worker validation API response: val_id={}, statusCode={}, body={}",
        valId,
        response.statusCode(),
        response.body());
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

  private void validateAgainstOrder(Order order, Map<String, Object> validated) {
    String tranId = normalizeTxn(asString(validated.get("tran_id")));
    String expectedTxn = normalizeTxn(order.getProviderTxnId());
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

  private void recordEvent(Order order, SslcommerzValidationJobMessage job, PaymentEventStatus status) {
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

  private void grantEnrollmentsForPaidOrder(UUID orderId) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    if (order.getStatus() != OrderStatus.PAID) {
      return;
    }
    Instant now = Instant.now();
    for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
      if (item.getItemType() == OrderItemType.COURSE) {
        activateOrCreateCourseEnrollment(order, item, item.getCourse(), now, item.getCollection());
        continue;
      }
      if (item.getItemType() == OrderItemType.COLLECTION) {
        activateOrCreateCollectionEnrollment(order, item, now);
        collectionCourseRepository.findByCollection_IdOrderByPositionAsc(item.getCollection().getId()).forEach(
            collectionCourse ->
                activateOrCreateCourseEnrollment(
                    order, item, collectionCourse.getCourse(), now, item.getCollection()));
      }
    }
  }

  private void activateOrCreateCourseEnrollment(
      Order order,
      OrderItem sourceOrderItem,
      com.gii.common.entity.course.Course course,
      Instant now,
      com.gii.common.entity.collection.Collection sourceCollection) {
    var existingOpt = enrollmentRepository.findByUserIdAndCourseId(order.getUser().getId(), course.getId());
    if (existingOpt.isPresent()) {
      Enrollment existing = existingOpt.get();
      existing.setStatus(EnrollmentStatus.ACTIVE);
      existing.setEnrolledAt(now);
      existing.setRevokedAt(null);
      existing.setExpiresAt(null);
      existing.setSourceOrderItem(sourceOrderItem);
      existing.setSourceCollection(sourceCollection);
      enrollmentRepository.save(existing);
      return;
    }
    Enrollment enrollment =
        Enrollment.builder()
            .user(order.getUser())
            .course(course)
            .sourceOrderItem(sourceOrderItem)
            .sourceCollection(sourceCollection)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(now)
            .build();
    saveEnrollmentIdempotent(enrollment);
  }

  private void activateOrCreateCollectionEnrollment(Order order, OrderItem sourceOrderItem, Instant now) {
    var existingOpt =
        collectionEnrollmentRepository.findByUserIdAndCollectionId(
            order.getUser().getId(), sourceOrderItem.getCollection().getId());
    if (existingOpt.isPresent()) {
      CollectionEnrollment existing = existingOpt.get();
      existing.setStatus(EnrollmentStatus.ACTIVE);
      existing.setEnrolledAt(now);
      existing.setRevokedAt(null);
      existing.setExpiresAt(null);
      existing.setSourceOrderItem(sourceOrderItem);
      collectionEnrollmentRepository.save(existing);
      return;
    }
    CollectionEnrollment collectionEnrollment =
        CollectionEnrollment.builder()
            .user(order.getUser())
            .collection(sourceOrderItem.getCollection())
            .sourceOrderItem(sourceOrderItem)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(now)
            .build();
    saveCollectionEnrollmentIdempotent(collectionEnrollment);
  }

  private void saveEnrollmentIdempotent(Enrollment enrollment) {
    try {
      enrollmentRepository.save(enrollment);
    } catch (DataIntegrityViolationException ignored) {
    }
  }

  private void saveCollectionEnrollmentIdempotent(CollectionEnrollment enrollment) {
    try {
      collectionEnrollmentRepository.save(enrollment);
    } catch (DataIntegrityViolationException ignored) {
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

  private record RawHttpResponse(int statusCode, String body) {}
}
