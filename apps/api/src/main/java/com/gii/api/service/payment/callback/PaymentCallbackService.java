package com.gii.api.service.payment.callback;

import com.gii.api.model.response.payment.PaymentStatusResponse;
import java.util.Map;
import java.util.UUID;

import com.gii.api.service.payment.PaymentFlowSupportService;
import com.gii.api.service.payment.webhook.PaymentSuccessWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentCallbackService {

  private final PaymentSuccessCallbackService paymentSuccessCallbackService;
  private final PaymentFailedCallbackService paymentFailedCallbackService;
  private final PaymentCancelledCallbackService paymentCancelledCallbackService;
  private final PaymentSuccessWebhookService paymentSuccessWebhookService;
  private final PaymentFlowSupportService paymentFlowSupportService;

  public PaymentStatusResponse success(UUID orderId, Map<String, String> queryParams) {
    return paymentSuccessCallbackService.execute(orderId, queryParams);
  }

  public PaymentStatusResponse successFromVerifiedWebhook(UUID orderId, Map<String, String> params) {
    return paymentSuccessWebhookService.execute(orderId, params);
  }

  public void grantEnrollmentsForPaidOrder(UUID orderId) {
    paymentFlowSupportService.grantEnrollmentsForPaidOrder(orderId);
  }

  public PaymentStatusResponse failed(UUID orderId, Map<String, String> queryParams) {
    return paymentFailedCallbackService.execute(orderId, queryParams);
  }

  public PaymentStatusResponse cancelled(UUID orderId, Map<String, String> queryParams) {
    return paymentCancelledCallbackService.execute(orderId, queryParams);
  }
}
