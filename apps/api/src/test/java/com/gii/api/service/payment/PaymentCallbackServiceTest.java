package com.gii.api.service.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackServiceTest {

  @Mock private OrderRepository orderRepository;
  @Mock private OrderItemRepository orderItemRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private PaymentEventRepository paymentEventRepository;
  @Mock private SslcommerzCallbackValidationService sslcommerzCallbackValidationService;
  @Mock private BkashCheckoutService bkashCheckoutService;

  @InjectMocks private PaymentCallbackService paymentCallbackService;

  @Test
  void successCallbackShouldVerifyAndEnrollForSslcommerz() {
    UUID orderId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User user = User.builder().email("s@example.com").build();
    user.setId(userId);
    Course course = Course.builder().title("course").build();
    course.setId(courseId);
    Order order =
        Order.builder()
            .user(user)
            .provider(OrderProvider.SSLCOMMERZ)
            .providerTxnId("txn-1")
            .amountBdt(new BigDecimal("1000"))
            .status(OrderStatus.PENDING)
            .build();
    order.setId(orderId);
    order.setCurrency("BDT");

    OrderItem item = OrderItem.builder().order(order).course(course).build();

    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));
    when(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.ACTIVE))
        .thenReturn(false);

    paymentCallbackService.success(orderId, Map.of("tran_id", "txn-1"));

    verify(sslcommerzCallbackValidationService).validateSuccessCallback(eq(order), any());
    verify(enrollmentRepository).save(any());
  }

  @Test
  void successFromVerifiedWebhookShouldEnroll() {
    UUID orderId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User user = User.builder().email("s@example.com").build();
    user.setId(userId);
    Course course = Course.builder().title("course").build();
    course.setId(courseId);
    Order order =
        Order.builder()
            .user(user)
            .provider(OrderProvider.BKASH)
            .providerTxnId("payment-1")
            .amountBdt(new BigDecimal("1000"))
            .status(OrderStatus.PENDING)
            .build();
    order.setId(orderId);
    order.setCurrency("BDT");
    OrderItem item = OrderItem.builder().order(order).course(course).build();

    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));
    when(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.ACTIVE))
        .thenReturn(false);

    paymentCallbackService.successFromVerifiedWebhook(orderId, Map.of("paymentID", "payment-1"));

    verify(enrollmentRepository).save(any());
    verify(sslcommerzCallbackValidationService, never()).validateSuccessCallback(any(), any());
    verify(bkashCheckoutService, never()).validateSuccessCallback(any(), any());
  }
}
