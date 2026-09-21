package com.gii.common.service.payment;

import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderStatus;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundedOrderEnrollmentService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CollectionEnrollmentRepository collectionEnrollmentRepository;

  @Transactional
  public void revoke(UUID orderId) {
    Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();
    if (order.getStatus() != OrderStatus.REFUNDED) {
      return;
    }

    Instant now = Instant.now();
    UUID userId = order.getUser().getId();
    for (Enrollment enrollment : enrollmentRepository.findBySourceOrderIdForUpdate(orderId)) {
      List<OrderItem> alternatives =
          orderItemRepository.findPaidCourseEntitlementsExcludingOrder(
              userId, enrollment.getCourse().getId(), orderId);
      if (!alternatives.isEmpty()) {
        OrderItem alternative = alternatives.getFirst();
        enrollment.setSourceOrderItem(alternative);
        enrollment.setSourceCollection(alternative.getCollection());
        continue;
      }
      enrollment.setStatus(EnrollmentStatus.REFUNDED);
      enrollment.setRevokedAt(now);
    }

    for (CollectionEnrollment enrollment :
        collectionEnrollmentRepository.findBySourceOrderIdForUpdate(orderId)) {
      List<OrderItem> alternatives =
          orderItemRepository.findPaidCollectionEntitlementsExcludingOrder(
              userId, enrollment.getCollection().getId(), orderId);
      if (!alternatives.isEmpty()) {
        enrollment.setSourceOrderItem(alternatives.getFirst());
        continue;
      }
      enrollment.setStatus(EnrollmentStatus.REFUNDED);
      enrollment.setRevokedAt(now);
    }
  }
}
