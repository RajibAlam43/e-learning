package com.gii.api.service.payment;

import com.gii.api.model.response.payment.PaymentStatusResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.order.Order;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderStatusService {

  private final CurrentUserService currentUserService;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final EnrollmentRepository enrollmentRepository;

  public PaymentStatusResponse execute(UUID orderId, Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    Order order =
        orderRepository
            .findByIdAndUserId(orderId, userId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

    int itemCount = orderItemRepository.findByOrderId(order.getId()).size();
    int enrolledCount =
        (int)
            orderItemRepository.findByOrderId(order.getId()).stream()
                .filter(
                    item ->
                        enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                            userId, item.getCourse().getId(), EnrollmentStatus.ACTIVE))
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
        .nextAction(nextAction(order.getStatus()))
        .actionUrl(nextActionUrl(order))
        .message(statusMessage(order.getStatus()))
        .build();
  }

  private String nextAction(OrderStatus status) {
    return switch (status) {
      case PAID -> "REDIRECT_TO_DASHBOARD";
      case PENDING -> "CONTINUE_PAYMENT";
      case FAILED, CANCELLED -> "RETRY_PAYMENT";
      case REFUNDED -> "CONTACT_SUPPORT";
    };
  }

  private String nextActionUrl(Order order) {
    if (order.getStatus() == OrderStatus.PENDING) {
      return "/payments/" + order.getId() + "/initiate";
    }
    if (order.getStatus() == OrderStatus.PAID) {
      return "/student/courses";
    }
    return null;
  }

  private String statusMessage(OrderStatus status) {
    return switch (status) {
      case PAID -> "Payment successful";
      case PENDING -> "Payment pending";
      case FAILED -> "Payment failed";
      case CANCELLED -> "Payment cancelled";
      case REFUNDED -> "Payment refunded";
    };
  }
}
