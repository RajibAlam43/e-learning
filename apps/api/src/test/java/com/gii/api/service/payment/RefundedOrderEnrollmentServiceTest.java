package com.gii.api.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gii.api.testsupport.CourseTestData;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderStatus;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.service.payment.RefundedOrderEnrollmentService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundedOrderEnrollmentServiceTest {

  @Mock private OrderRepository orderRepository;
  @Mock private OrderItemRepository orderItemRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private CollectionEnrollmentRepository collectionEnrollmentRepository;

  @InjectMocks private RefundedOrderEnrollmentService service;

  @Test
  void refundRevokesAccessWithNoOtherPaidEntitlement() {
    UUID orderId = UUID.randomUUID();
    User user = user();
    Order order = Order.builder().user(user).status(OrderStatus.REFUNDED).build();
    order.setId(orderId);
    Course course = CourseTestData.course("Course", "course", user);
    course.setId(UUID.randomUUID());
    Enrollment enrollment =
        Enrollment.builder()
            .user(user)
            .course(course)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now())
            .build();
    when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
    when(enrollmentRepository.findBySourceOrderIdForUpdate(orderId))
        .thenReturn(List.of(enrollment));
    when(collectionEnrollmentRepository.findBySourceOrderIdForUpdate(orderId))
        .thenReturn(List.of());
    when(orderItemRepository.findPaidCourseEntitlementsExcludingOrder(
            user.getId(), course.getId(), orderId))
        .thenReturn(List.of());

    service.revoke(orderId);

    assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.REFUNDED);
    assertThat(enrollment.getRevokedAt()).isNotNull();
  }

  @Test
  void refundKeepsAccessWhenAnotherPaidOrderCoversCourse() {
    UUID orderId = UUID.randomUUID();
    User user = user();
    Order order = Order.builder().user(user).status(OrderStatus.REFUNDED).build();
    order.setId(orderId);
    Course course = CourseTestData.course("Course", "course", user);
    course.setId(UUID.randomUUID());
    Enrollment enrollment =
        Enrollment.builder()
            .user(user)
            .course(course)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now())
            .build();
    OrderItem alternative =
        OrderItem.builder().itemType(OrderItemType.COURSE).course(course).build();
    alternative.setId(UUID.randomUUID());
    when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
    when(enrollmentRepository.findBySourceOrderIdForUpdate(orderId))
        .thenReturn(List.of(enrollment));
    when(collectionEnrollmentRepository.findBySourceOrderIdForUpdate(orderId))
        .thenReturn(List.of());
    when(orderItemRepository.findPaidCourseEntitlementsExcludingOrder(
            user.getId(), course.getId(), orderId))
        .thenReturn(List.of(alternative));

    service.revoke(orderId);

    assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(enrollment.getSourceOrderItem()).isSameAs(alternative);
    assertThat(enrollment.getRevokedAt()).isNull();
  }

  private User user() {
    User user = User.builder().fullName("Student").email("student@example.com").build();
    user.setId(UUID.randomUUID());
    return user;
  }
}
