package com.gii.api.service.payment;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gii.api.model.response.payment.PaymentStatusResponse;
import com.gii.api.service.payment.callback.PaymentCallbackService;
import com.gii.api.service.payment.callback.PaymentCancelledCallbackService;
import com.gii.api.service.payment.callback.PaymentFailedCallbackService;
import com.gii.api.service.payment.callback.PaymentSuccessCallbackService;
import com.gii.api.service.payment.webhook.PaymentSuccessWebhookService;
import com.gii.common.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackServiceTest {

  @Mock private PaymentSuccessCallbackService paymentSuccessCallbackService;
  @Mock private PaymentFailedCallbackService paymentFailedCallbackService;
  @Mock private PaymentCancelledCallbackService paymentCancelledCallbackService;
  @Mock private PaymentSuccessWebhookService paymentSuccessWebhookService;
  @Mock private PaymentFlowSupportService paymentFlowSupportService;

  @InjectMocks private PaymentCallbackService paymentCallbackService;

  @Test
  void successShouldDelegateToSuccessCallbackService() {
    UUID orderId = UUID.randomUUID();
    Map<String, String> params = Map.of("tran_id", "txn-1");

    paymentCallbackService.success(orderId, params);

    verify(paymentSuccessCallbackService).execute(orderId, params);
  }

  @Test
  void successFromVerifiedWebhookShouldDelegateToWebhookService() {
    UUID orderId = UUID.randomUUID();
    Map<String, String> params = Map.of("paymentID", "payment-1");
    when(paymentSuccessWebhookService.execute(orderId, params)).thenReturn(dummyStatus(orderId));

    paymentCallbackService.successFromVerifiedWebhook(orderId, params);

    verify(paymentSuccessWebhookService).execute(orderId, params);
  }

  @Test
  void failedShouldDelegateToFailedCallbackService() {
    UUID orderId = UUID.randomUUID();
    Map<String, String> params = Map.of("tran_id", "txn-2");

    paymentCallbackService.failed(orderId, params);

    verify(paymentFailedCallbackService).execute(orderId, params);
  }

  @Test
  void cancelledShouldDelegateToCancelledCallbackService() {
    UUID orderId = UUID.randomUUID();
    Map<String, String> params = Map.of("tran_id", "txn-3");

    paymentCallbackService.cancelled(orderId, params);

    verify(paymentCancelledCallbackService).execute(orderId, params);
  }

  @Test
  void grantEnrollmentsShouldDelegateToFlowSupportService() {
    UUID orderId = UUID.randomUUID();

    paymentCallbackService.grantEnrollmentsForPaidOrder(orderId);

    verify(paymentFlowSupportService).grantEnrollmentsForPaidOrder(orderId);
  }

  private PaymentStatusResponse dummyStatus(UUID orderId) {
    return PaymentStatusResponse.builder()
        .orderId(orderId)
        .status(OrderStatus.PAID)
        .totalAmount(BigDecimal.TEN)
        .currency("BDT")
        .createdAt(Instant.now())
        .build();
  }
}
