package com.gii.api.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.PaymentAttempt;
import com.gii.common.enums.OrderProvider;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentAttemptRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import com.gii.common.service.payment.PaidOrderEnrollmentService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentFlowSupportServiceTest {

  @Test
  void terminalCallbackForHistoricalAttemptShouldNotReplaceCurrentAttempt() {
    OrderRepository orderRepository = mock(OrderRepository.class);
    PaymentAttemptRepository attemptRepository = mock(PaymentAttemptRepository.class);
    PaymentFlowSupportService service = service(orderRepository, attemptRepository);
    UUID orderId = UUID.randomUUID();
    Order order =
        Order.builder()
            .id(orderId)
            .provider(OrderProvider.BKASH)
            .providerTxnId("current-attempt")
            .build();
    PaymentAttempt historicalAttempt =
        PaymentAttempt.builder()
            .order(order)
            .provider(OrderProvider.BKASH)
            .providerTxnId("historical-attempt")
            .build();
    when(attemptRepository.findTopByOrderIdAndProviderAndProviderTxnIdOrderByCreatedAtDesc(
            orderId, OrderProvider.BKASH, "historical-attempt"))
        .thenReturn(Optional.of(historicalAttempt));

    boolean current =
        service.validateTerminalProviderTransactionId(
            order, OrderProvider.BKASH, "historical-attempt");

    assertThat(current).isFalse();
    assertThat(order.getProvider()).isEqualTo(OrderProvider.BKASH);
    assertThat(order.getProviderTxnId()).isEqualTo("current-attempt");
    verify(orderRepository, never()).save(order);
  }

  @Test
  void terminalCallbackForCurrentAttemptShouldBeCurrent() {
    OrderRepository orderRepository = mock(OrderRepository.class);
    PaymentAttemptRepository attemptRepository = mock(PaymentAttemptRepository.class);
    PaymentFlowSupportService service = service(orderRepository, attemptRepository);
    Order order =
        Order.builder()
            .id(UUID.randomUUID())
            .provider(OrderProvider.SSLCOMMERZ)
            .providerTxnId("current-attempt")
            .build();

    boolean current =
        service.validateTerminalProviderTransactionId(
            order, OrderProvider.SSLCOMMERZ, "current-attempt");

    assertThat(current).isTrue();
    verify(orderRepository, never()).save(order);
  }

  @Test
  void successValidationShouldContinueToPromoteHistoricalAttempt() {
    OrderRepository orderRepository = mock(OrderRepository.class);
    PaymentAttemptRepository attemptRepository = mock(PaymentAttemptRepository.class);
    PaymentFlowSupportService service = service(orderRepository, attemptRepository);
    UUID orderId = UUID.randomUUID();
    Order order =
        Order.builder()
            .id(orderId)
            .provider(OrderProvider.BKASH)
            .providerTxnId("current-attempt")
            .build();
    PaymentAttempt historicalAttempt =
        PaymentAttempt.builder()
            .order(order)
            .provider(OrderProvider.SSLCOMMERZ)
            .providerTxnId("successful-historical-attempt")
            .build();
    when(attemptRepository.findTopByOrderIdAndProviderAndProviderTxnIdOrderByCreatedAtDesc(
            orderId, OrderProvider.SSLCOMMERZ, "successful-historical-attempt"))
        .thenReturn(Optional.of(historicalAttempt));

    service.validateProviderTransactionId(
        order, OrderProvider.SSLCOMMERZ, "successful-historical-attempt");

    assertThat(order.getProvider()).isEqualTo(OrderProvider.SSLCOMMERZ);
    assertThat(order.getProviderTxnId()).isEqualTo("successful-historical-attempt");
    verify(orderRepository).save(order);
  }

  private PaymentFlowSupportService service(
      OrderRepository orderRepository, PaymentAttemptRepository attemptRepository) {
    return new PaymentFlowSupportService(
        orderRepository,
        mock(OrderItemRepository.class),
        mock(EnrollmentRepository.class),
        mock(CollectionEnrollmentRepository.class),
        mock(PaymentEventRepository.class),
        attemptRepository,
        mock(PaidOrderEnrollmentService.class));
  }
}
