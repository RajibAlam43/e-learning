package com.gii.api.service.payment;

import com.gii.api.model.response.payment.PaymentStatusResponse;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PaymentEventType;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentAttemptRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import com.gii.common.service.payment.PaidOrderEnrollmentService;
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
  private final CollectionEnrollmentRepository collectionEnrollmentRepository;
  private final PaymentEventRepository paymentEventRepository;
  private final PaymentAttemptRepository paymentAttemptRepository;
  private final PaidOrderEnrollmentService paidOrderEnrollmentService;

  public Order requireOrder(UUID orderId) {
    return orderRepository
        .findByIdForUpdate(orderId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
  }

  public void validateProviderTransactionId(
      Order order, OrderProvider callbackProvider, String callbackTxnId) {
    validateProviderTransactionId(order, callbackProvider, callbackTxnId, true);
  }

  private boolean validateProviderTransactionId(
      Order order,
      OrderProvider callbackProvider,
      String callbackTxnId,
      boolean promoteHistoricalAttempt) {
    if (callbackTxnId == null || callbackTxnId.isBlank()) {
      return true;
    }
    String expected = normalizeTxn(order.getProviderTxnId());
    String actual = normalizeTxn(callbackTxnId);
    boolean currentAttempt = order.getProvider() == callbackProvider && expected.equals(actual);
    boolean matchesKnownAttempt = currentAttempt;
    if (!currentAttempt) {
      var attempt =
          paymentAttemptRepository
              .findTopByOrderIdAndProviderAndProviderTxnIdOrderByCreatedAtDesc(
                  order.getId(), callbackProvider, callbackTxnId)
              .orElse(null);
      matchesKnownAttempt = attempt != null;
      if (attempt != null && promoteHistoricalAttempt) {
        order.setProvider(attempt.getProvider());
        order.setProviderTxnId(attempt.getProviderTxnId());
        orderRepository.save(order);
      }
    }
    if (!matchesKnownAttempt) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Callback transaction identifier does not match order");
    }
    return currentAttempt;
  }

  public boolean validateTerminalProviderTransactionId(
      Order order, OrderProvider callbackProvider, String callbackTxnId) {
    return validateProviderTransactionId(order, callbackProvider, callbackTxnId, false);
  }

  private String normalizeTxn(String value) {
    return value == null ? "" : value.replace("-", "").trim().toLowerCase();
  }

  public void recordCallbackEvent(
      Order order,
      PaymentEventType eventType,
      Map<String, String> payload,
      PaymentEventStatus status) {
    recordCallbackEvent(order, order.getProvider(), eventType, payload, status);
  }

  public void recordCallbackEvent(
      Order order,
      OrderProvider callbackProvider,
      PaymentEventType eventType,
      Map<String, String> payload,
      PaymentEventStatus status) {
    String providerEventId =
        firstNonBlank(payload.get("event_id"), payload.get("eventId"), payload.get("event_ref"));
    PaymentEvent event =
        PaymentEvent.builder()
            .order(order)
            .provider(callbackProvider)
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

  public void markPaid(Order order) {
    if (order.getStatus() == OrderStatus.PAID) {
      return;
    }
    if (order.getStatus() == OrderStatus.REFUNDED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not payable");
    }
    order.setStatus(OrderStatus.PAID);
    if (order.getPaidAt() == null) {
      order.setPaidAt(Instant.now());
    }
    orderRepository.save(order);
  }

  public void markPaidAndGrant(Order order) {
    markPaid(order);
    paidOrderEnrollmentService.grant(order.getId());
  }

  public void grantEnrollmentsForPaidOrder(UUID orderId) {
    paidOrderEnrollmentService.grant(orderId);
  }

  public PaymentStatusResponse transitionFailedAndBuild(Order order) {
    if (order.getStatus() == OrderStatus.PENDING) {
      order.setStatus(OrderStatus.FAILED);
      order = orderRepository.save(order);
    }
    return toStatus(order);
  }

  public void transitionFailed(Order order) {
    if (order.getStatus() == OrderStatus.PENDING) {
      order.setStatus(OrderStatus.FAILED);
      orderRepository.save(order);
    }
  }

  public PaymentStatusResponse transitionCancelledAndBuild(Order order) {
    if (order.getStatus() == OrderStatus.PENDING) {
      order.setStatus(OrderStatus.CANCELLED);
      order = orderRepository.save(order);
    }
    return toStatus(order);
  }

  public void transitionCancelled(Order order) {
    if (order.getStatus() == OrderStatus.PENDING) {
      order.setStatus(OrderStatus.CANCELLED);
      orderRepository.save(order);
    }
  }

  public PaymentStatusResponse toStatus(Order order) {
    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
    int itemCount = items.size();
    int enrolledCount =
        (int)
            items.stream()
                .filter(
                    item -> {
                      if (item.getItemType() == OrderItemType.COURSE) {
                        return enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                            order.getUser().getId(),
                            item.getCourse().getId(),
                            EnrollmentStatus.ACTIVE);
                      }
                      if (item.getItemType() == OrderItemType.COLLECTION) {
                        return collectionEnrollmentRepository
                            .existsByUserIdAndCollectionIdAndStatus(
                                order.getUser().getId(),
                                item.getCollection().getId(),
                                EnrollmentStatus.ACTIVE);
                      }
                      return false;
                    })
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
