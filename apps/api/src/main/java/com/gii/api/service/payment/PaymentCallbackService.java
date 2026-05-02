package com.gii.api.service.payment;

import com.gii.api.model.response.payment.PaymentStatusResponse;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentCallbackService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final PaymentEventRepository paymentEventRepository;

  public PaymentStatusResponse success(UUID orderId, Map<String, String> queryParams) {
    String providerEventId =
        firstNonBlank(queryParams.get("tran_id"), queryParams.get("payment_id"));
    if (providerEventId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Missing required callback transaction identifier");
    }
    Order order = requireOrder(orderId);
    validateProviderTransactionId(order, providerEventId);
    recordCallbackEvent(order, "callback_success", queryParams, PaymentEventStatus.PROCESSED);

    // Idempotent success transition.
    if (order.getStatus() == OrderStatus.PAID) {
      return toStatus(order);
    }
    if (order.getStatus() == OrderStatus.REFUNDED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not payable");
    }

    order.setStatus(OrderStatus.PAID);
    if (order.getPaidAt() == null) {
      order.setPaidAt(Instant.now());
    }
    Order savedOrder = orderRepository.save(order);
    grantEnrollments(savedOrder);
    return toStatus(savedOrder);
  }

  public PaymentStatusResponse failed(UUID orderId, Map<String, String> queryParams) {
    Order order = requireOrder(orderId);
    String providerEventId =
        firstNonBlank(queryParams.get("tran_id"), queryParams.get("payment_id"));
    validateProviderTransactionId(order, providerEventId);
    recordCallbackEvent(order, "callback_failed", queryParams, PaymentEventStatus.PROCESSED);
    if (order.getStatus() == OrderStatus.PENDING) {
      order.setStatus(OrderStatus.FAILED);
      order = orderRepository.save(order);
    }
    return toStatus(order);
  }

  public PaymentStatusResponse cancelled(UUID orderId, Map<String, String> queryParams) {
    Order order = requireOrder(orderId);
    String providerEventId =
        firstNonBlank(queryParams.get("tran_id"), queryParams.get("payment_id"));
    validateProviderTransactionId(order, providerEventId);
    recordCallbackEvent(order, "callback_cancelled", queryParams, PaymentEventStatus.PROCESSED);
    if (order.getStatus() == OrderStatus.PENDING) {
      order.setStatus(OrderStatus.CANCELLED);
      order = orderRepository.save(order);
    }
    return toStatus(order);
  }

  private void grantEnrollments(Order order) {
    Instant now = Instant.now();
    for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
      if (enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
          order.getUser().getId(), item.getCourse().getId(), EnrollmentStatus.ACTIVE)) {
        continue;
      }
      Enrollment enrollment =
          Enrollment.builder()
              .user(order.getUser())
              .course(item.getCourse())
              .status(EnrollmentStatus.ACTIVE)
              .enrolledAt(now)
              .build();
      enrollmentRepository.save(enrollment);
    }
  }

  private Order requireOrder(UUID orderId) {
    return orderRepository
        .findById(orderId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
  }

  private void recordCallbackEvent(
      Order order, String eventType, Map<String, String> payload, PaymentEventStatus status) {
    String providerEventId =
        firstNonBlank(payload.get("event_id"), payload.get("eventId"), payload.get("event_ref"));
    PaymentEvent event =
        PaymentEvent.builder()
            .order(order)
            .provider(order.getProvider())
            .eventType(eventType)
            .providerEventId(providerEventId)
            .rawPayloadJson(Map.copyOf(payload))
            .status(status)
            .processedAt(Instant.now())
            .build();
    paymentEventRepository.save(event);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private void validateProviderTransactionId(Order order, String callbackTxnId) {
    if (callbackTxnId == null || callbackTxnId.isBlank()) {
      return;
    }
    if (order.getProviderTxnId() == null || order.getProviderTxnId().isBlank()) {
      return;
    }
    if (!order.getProviderTxnId().equals(callbackTxnId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Callback transaction identifier does not match order");
    }
  }

  private PaymentStatusResponse toStatus(Order order) {
    int enrolledCount =
        (int)
            orderItemRepository.findByOrderId(order.getId()).stream()
                .filter(
                    item ->
                        enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                            order.getUser().getId(),
                            item.getCourse().getId(),
                            EnrollmentStatus.ACTIVE))
                .count();
    return PaymentStatusResponse.builder()
        .orderId(order.getId())
        .status(order.getStatus())
        .totalAmount(order.getAmountBdt())
        .currency(order.getCurrency())
        .provider(order.getProvider())
        .providerTransactionId(order.getProviderTxnId())
        .createdAt(order.getCreatedAt())
        .paidAt(order.getPaidAt())
        .refundedAt(order.getRefundedAt())
        .customerEmail(order.getUser().getEmail())
        .customerPhone(order.getUser().getPhone())
        .coursesEnrolled(order.getStatus() == OrderStatus.PAID)
        .enrolledCourseCount(enrolledCount)
        .nextAction(
            order.getStatus() == OrderStatus.PAID ? "REDIRECT_TO_DASHBOARD" : "INITIATE_PAYMENT")
        .actionUrl(
            order.getStatus() == OrderStatus.PAID
                ? "/student/courses"
                : "/payments/" + order.getId() + "/initiate")
        .message(
            order.getStatus() == OrderStatus.PAID ? "Payment successful" : "Payment state updated")
        .build();
  }
}
