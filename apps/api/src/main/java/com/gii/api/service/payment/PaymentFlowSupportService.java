package com.gii.api.service.payment;

import com.gii.api.model.response.payment.PaymentStatusResponse;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PaymentEventType;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import java.time.Instant;
import java.util.List;
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
public class PaymentFlowSupportService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final PaymentEventRepository paymentEventRepository;

  public Order requireOrder(UUID orderId) {
    return orderRepository
        .findById(orderId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
  }

  public void validateProviderTransactionId(Order order, String callbackTxnId) {
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

  public void recordCallbackEvent(
          Order order, PaymentEventType eventType, Map<String, String> payload, PaymentEventStatus status) {
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

  public PaymentStatusResponse markPaidAndBuildResponse(Order order) {
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
    return toStatus(orderRepository.save(order));
  }

  public void grantEnrollmentsForPaidOrder(UUID orderId) {
    Order order = requireOrder(orderId);
    if (order.getStatus() != OrderStatus.PAID) {
      return;
    }
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

  public PaymentStatusResponse transitionFailedAndBuild(Order order) {
    if (order.getStatus() == OrderStatus.PENDING) {
      order.setStatus(OrderStatus.FAILED);
      order = orderRepository.save(order);
    }
    return toStatus(order);
  }

  public PaymentStatusResponse transitionCancelledAndBuild(Order order) {
    if (order.getStatus() == OrderStatus.PENDING) {
      order.setStatus(OrderStatus.CANCELLED);
      order = orderRepository.save(order);
    }
    return toStatus(order);
  }

  public PaymentStatusResponse toStatus(Order order) {
    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
    int itemCount = items.size();
    int enrolledCount =
        (int)
            items.stream()
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
        .coursesEnrolled(enrolledCount == itemCount && itemCount > 0)
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

  public String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }
}
