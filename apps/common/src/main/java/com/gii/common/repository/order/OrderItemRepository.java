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
      value =
          """
          SELECT oi.* FROM order_items oi JOIN orders o ON o.id=oi.order_id
          WHERE o.user_id=:userId AND o.status='PAID' AND o.id<>:excludedOrderId
          AND (
            (oi.item_type='COURSE' AND oi.course_offering_id=:courseId)
            OR (oi.item_type='COLLECTION' AND oi.program_snapshot->>'requiredCredits' IS NULL
              AND EXISTS(SELECT 1 FROM order_item_courses s
                WHERE s.order_item_id=oi.id AND s.course_offering_id=:courseId))
          ) ORDER BY o.paid_at DESC,oi.id DESC
          """,
      nativeQuery = true)
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
      value =
          """
          SELECT COUNT(DISTINCT o.id) FROM order_items oi JOIN orders o ON o.id=oi.order_id
          WHERE o.status='PENDING' AND o.created_at>:createdAfter AND o.user_id<>:userId
          AND (
            (oi.item_type='COURSE' AND oi.course_offering_id=:courseId)
            OR (oi.item_type='COLLECTION' AND oi.program_snapshot->>'requiredCredits' IS NULL
              AND EXISTS(SELECT 1 FROM order_item_courses s
                WHERE s.order_item_id=oi.id AND s.course_offering_id=:courseId))
          )
          """,
      nativeQuery = true)
  long countPendingReservations(
      @Param("courseId") UUID courseId,
      @Param("userId") UUID userId,
      @Param("createdAfter") java.time.Instant createdAfter);
}
