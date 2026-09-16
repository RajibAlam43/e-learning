package com.gii.common.repository.order;

import com.gii.common.entity.order.OrderItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

  List<OrderItem> findByOrderId(UUID orderId);

  boolean existsByCollectionId(UUID collectionId);

  @Query(
      """
      SELECT oi
      FROM OrderItem oi
      WHERE oi.order.user.id = :userId
      AND oi.order.status = com.gii.common.enums.OrderStatus.PAID
      AND oi.order.id <> :excludedOrderId
      AND (
        (oi.itemType = com.gii.common.enums.OrderItemType.COURSE AND oi.course.id = :courseId)
        OR
        (oi.itemType = com.gii.common.enums.OrderItemType.COLLECTION AND EXISTS (
          SELECT snapshot.id FROM OrderItemCourse snapshot
          WHERE snapshot.orderItem.id = oi.id AND snapshot.course.id = :courseId
        ))
      )
      ORDER BY oi.order.paidAt DESC, oi.id DESC
      """)
  List<OrderItem> findPaidCourseEntitlementsExcludingOrder(
      @Param("userId") UUID userId,
      @Param("courseId") UUID courseId,
      @Param("excludedOrderId") UUID excludedOrderId);

  @Query(
      """
      SELECT oi
      FROM OrderItem oi
      WHERE oi.order.user.id = :userId
      AND oi.order.status = com.gii.common.enums.OrderStatus.PAID
      AND oi.order.id <> :excludedOrderId
      AND oi.itemType = com.gii.common.enums.OrderItemType.COLLECTION
      AND oi.collection.id = :collectionId
      ORDER BY oi.order.paidAt DESC, oi.id DESC
      """)
  List<OrderItem> findPaidCollectionEntitlementsExcludingOrder(
      @Param("userId") UUID userId,
      @Param("collectionId") UUID collectionId,
      @Param("excludedOrderId") UUID excludedOrderId);

  @Query(
      """
      SELECT COUNT(DISTINCT oi.order.id)
      FROM OrderItem oi
      WHERE oi.order.status = com.gii.common.enums.OrderStatus.PENDING
      AND oi.order.createdAt > :createdAfter
      AND oi.order.user.id <> :userId
      AND (
        (oi.itemType = com.gii.common.enums.OrderItemType.COURSE AND oi.course.id = :courseId)
        OR
        (oi.itemType = com.gii.common.enums.OrderItemType.COLLECTION AND EXISTS (
          SELECT snapshot.id FROM OrderItemCourse snapshot
          WHERE snapshot.orderItem.id = oi.id AND snapshot.course.id = :courseId
        ))
      )
      """)
  long countPendingReservations(
      @Param("courseId") UUID courseId,
      @Param("userId") UUID userId,
      @Param("createdAfter") java.time.Instant createdAfter);
}
