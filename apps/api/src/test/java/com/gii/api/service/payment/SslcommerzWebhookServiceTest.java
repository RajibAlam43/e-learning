package com.gii.api.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gii.api.model.response.payment.WebhookAckResponse;
import com.gii.api.service.payment.callback.SslcommerzCallbackService;
import com.gii.api.service.payment.sslcommerz.SslcommerzCallbackValidationService;
import com.gii.api.service.payment.sslcommerz.SslcommerzWebhookService;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.entity.user.User;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SslcommerzWebhookServiceTest {

  @Mock private PaymentEventRepository paymentEventRepository;
  @Mock private OrderRepository orderRepository;
  @Mock private SslcommerzCallbackService sslcommerzCallbackService;
  @Mock private SslcommerzCallbackValidationService sslcommerzCallbackValidationService;

  @InjectMocks private SslcommerzWebhookService service;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "validateOnWebhook", true);
  }

  @Test
  void validWebhookWithRiskLevelOneShouldBeHeld() {
    UUID orderId = UUID.randomUUID();
    Order order = sslOrder(orderId, "txn-ssl-risk");

    String payload =
        "tran_id=txn-ssl-risk&status=VALID&val_id=val-1&verify_key=status,tran_id,val_id&verify_sign=abc";
    when(paymentEventRepository.findByProviderAndProviderEventId(
            OrderProvider.SSLCOMMERZ, "evt-risk-1"))
        .thenReturn(Optional.empty());
    when(orderRepository.findByProviderAndProviderTxnId(OrderProvider.SSLCOMMERZ, "txn-ssl-risk"))
        .thenReturn(Optional.of(order));
    when(sslcommerzCallbackValidationService.validateIpnNotification(eq(order), any()))
        .thenReturn(new SslcommerzCallbackValidationService.ValidationOutcome("VALID", 1));
    when(paymentEventRepository.save(any(PaymentEvent.class)))
        .thenAnswer(
            inv -> {
              PaymentEvent event = inv.getArgument(0);
              event.setId(UUID.randomUUID());
              return event;
            });

    WebhookAckResponse response =
        service.handle(Map.of("x-event-id", "evt-risk-1"), toParams(payload));

    verify(sslcommerzCallbackService, never()).successFromWebhook(any(), any());
    assertThat(response.message()).contains("held");
  }

  @Test
  void validatedFailedStatusShouldTriggerFailedTransition() {
    UUID orderId = UUID.randomUUID();
    Order order = sslOrder(orderId, "txn-ssl-failed");

    String payload =
        "tran_id=txn-ssl-failed&status=VALID&val_id=val-2&verify_key=status,tran_id,val_id&verify_sign=abc";
    when(paymentEventRepository.findByProviderAndProviderEventId(
            OrderProvider.SSLCOMMERZ, "evt-failed-1"))
        .thenReturn(Optional.empty());
    when(orderRepository.findByProviderAndProviderTxnId(OrderProvider.SSLCOMMERZ, "txn-ssl-failed"))
        .thenReturn(Optional.of(order));
    when(sslcommerzCallbackValidationService.validateIpnNotification(eq(order), any()))
        .thenReturn(new SslcommerzCallbackValidationService.ValidationOutcome("FAILED", 0));
    when(paymentEventRepository.save(any(PaymentEvent.class)))
        .thenAnswer(
            inv -> {
              PaymentEvent event = inv.getArgument(0);
              event.setId(UUID.randomUUID());
              return event;
            });

    WebhookAckResponse response =
        service.handle(Map.of("x-event-id", "evt-failed-1"), toParams(payload));

    verify(sslcommerzCallbackService).failedFromWebhook(eq(orderId), any());
    verify(sslcommerzCallbackService, never()).successFromWebhook(any(), any());
    assertThat(response.acknowledged()).isTrue();
  }

  private Order sslOrder(UUID orderId, String txnId) {
    User user = User.builder().fullName("Student").email("s@example.com").passwordHash("x").build();
    user.setId(UUID.randomUUID());
    Order order =
        Order.builder()
            .user(user)
            .provider(OrderProvider.SSLCOMMERZ)
            .providerTxnId(txnId)
            .amountBdt(BigDecimal.valueOf(1000))
            .currency("BDT")
            .status(OrderStatus.PENDING)
            .createdAt(Instant.now())
            .build();
    order.setId(orderId);
    return order;
  }

  private Map<String, String> toParams(String payload) {
    Map<String, String> out = new HashMap<>();
    for (String pair : payload.split("&")) {
      if (pair == null || pair.isBlank()) {
        continue;
      }
      int idx = pair.indexOf('=');
      if (idx < 0) {
        out.put(pair, "");
      } else {
        out.put(pair.substring(0, idx), pair.substring(idx + 1));
      }
    }
    return out;
  }
}
