package com.gii.common.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.gii.api.testsupport.CourseTestData;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemCourseRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaidOrderEnrollmentServiceTest {

  @Mock private OrderRepository orderRepository;
  @Mock private OrderItemRepository orderItemRepository;
  @Mock private OrderItemCourseRepository orderItemCourseRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private CollectionEnrollmentRepository collectionEnrollmentRepository;
  @Mock private CollectionCourseRepository collectionCourseRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private CourseEnrollmentPolicyService enrollmentPolicyService;

  private PaidOrderEnrollmentService service;

  private PaidOrderEnrollmentService newService() {
    return new PaidOrderEnrollmentService(
        orderRepository,
        orderItemRepository,
        orderItemCourseRepository,
        enrollmentRepository,
        collectionEnrollmentRepository,
        collectionCourseRepository,
        courseRepository,
        enrollmentPolicyService);
  }

  @Test
  void reEnrollmentPreservesCompletedAtOnRefundedEnrollment() {
    assertReEnrollmentPreservesCompletedAt(
        EnrollmentStatus.REFUNDED, Instant.now().plusSeconds(86_400));
  }

  @Test
  void reEnrollmentPreservesCompletedAtOnRevokedEnrollment() {
    assertReEnrollmentPreservesCompletedAt(
        EnrollmentStatus.REVOKED, Instant.now().plusSeconds(86_400));
  }

  @Test
  void reEnrollmentPreservesCompletedAtOnExpiredEnrollment() {
    assertReEnrollmentPreservesCompletedAt(
        EnrollmentStatus.ACTIVE, Instant.now().minusSeconds(60));
  }

  private void assertReEnrollmentPreservesCompletedAt(
      EnrollmentStatus existingStatus, Instant existingExpiresAt) {
    service = newService();

    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();

    User user = User.builder().fullName("Student").email("student@example.com").build();
    user.setId(userId);

    Course course = CourseTestData.course("Paid course", "paid-course", user);
    course.setId(courseId);
    course.setStatus(PublishStatus.PUBLISHED);
    course.setIsFree(false);

    Order order = Order.builder().user(user).amountBdt(BigDecimal.TEN).status(OrderStatus.PAID).build();
    order.setId(orderId);
    order.setCreatedAt(Instant.now().minusSeconds(700_000));

    OrderItem item =
        OrderItem.builder()
            .order(order)
            .course(course)
            .itemType(OrderItemType.COURSE)
            .titleSnapshot("Paid course")
            .priceBdt(BigDecimal.TEN)
            .build();

    Instant completedAt = Instant.now().minusSeconds(500_000);
    Enrollment existing =
        Enrollment.builder()
            .user(user)
            .course(course)
            .status(existingStatus)
            .enrolledAt(Instant.now().minusSeconds(600_000))
            .revokedAt(existingStatus == EnrollmentStatus.REVOKED ? Instant.now() : null)
            .completedAt(completedAt)
            .expiresAt(existingExpiresAt)
            .build();

    Instant newExpiry = Instant.now().plusSeconds(86_400);
    when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
    when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));
    when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
    when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId))
        .thenReturn(Optional.of(existing));
    when(enrollmentPolicyService.calculateExpiry(any(), any())).thenReturn(newExpiry);

    service.grant(orderId);

    assertThat(existing.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(existing.getRevokedAt()).isNull();
    assertThat(existing.getExpiresAt()).isEqualTo(newExpiry);
    assertThat(existing.getCompletedAt()).isEqualTo(completedAt);
  }

  @Test
  void collectionReEnrollmentPreservesCompletedAtOnRefundedEnrollment() {
    assertCollectionReEnrollmentPreservesCompletedAt(EnrollmentStatus.REFUNDED);
  }

  @Test
  void collectionReEnrollmentPreservesCompletedAtOnRevokedEnrollment() {
    assertCollectionReEnrollmentPreservesCompletedAt(EnrollmentStatus.REVOKED);
  }

  private void assertCollectionReEnrollmentPreservesCompletedAt(EnrollmentStatus existingStatus) {
    service = newService();

    UUID userId = UUID.randomUUID();
    UUID collectionId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();

    User user = User.builder().fullName("Student").email("student@example.com").build();
    user.setId(userId);

    Order order = Order.builder().user(user).amountBdt(BigDecimal.TEN).status(OrderStatus.PAID).build();
    order.setId(orderId);
    order.setCreatedAt(Instant.now().minusSeconds(700_000));

    Collection collection =
        Collection.builder().title("Pack").slug("pack").type(CollectionType.PACK).build();
    collection.setId(collectionId);

    OrderItem item =
        OrderItem.builder()
            .order(order)
            .collection(collection)
            .itemType(OrderItemType.COLLECTION)
            .titleSnapshot("Pack")
            .priceBdt(BigDecimal.TEN)
            .build();

    Instant completedAt = Instant.now().minusSeconds(500_000);
    CollectionEnrollment existing =
        CollectionEnrollment.builder()
            .user(user)
            .collection(collection)
            .status(existingStatus)
            .enrolledAt(Instant.now().minusSeconds(600_000))
            .revokedAt(existingStatus == EnrollmentStatus.REVOKED ? Instant.now() : null)
            .completedAt(completedAt)
            .build();

    when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
    when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));
    when(orderItemCourseRepository.findByOrderItemIdOrderByPositionAsc(any()))
        .thenReturn(List.of());
    when(collectionCourseRepository.findByCollection_IdOrderByPositionAsc(collectionId))
        .thenReturn(List.of());
    when(collectionEnrollmentRepository.findByUserIdAndCollectionId(userId, collectionId))
        .thenReturn(Optional.of(existing));

    service.grant(orderId);

    assertThat(existing.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(existing.getRevokedAt()).isNull();
    assertThat(existing.getCompletedAt()).isEqualTo(completedAt);
  }
}
