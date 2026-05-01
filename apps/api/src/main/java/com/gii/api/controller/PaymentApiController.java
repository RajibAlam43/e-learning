package com.gii.api.controller;

import com.gii.api.model.request.payment.InitiatePaymentRequest;
import com.gii.api.model.response.payment.CheckoutOrderResponse;
import com.gii.api.model.response.payment.PaymentInitiationResponse;
import com.gii.api.model.response.payment.PaymentStatusResponse;
import com.gii.api.model.response.payment.ReceiptResponse;
import com.gii.api.model.response.payment.WebhookAckResponse;
import com.gii.api.service.payment.InitiatePaymentService;
import com.gii.api.service.payment.OrderStatusService;
import com.gii.api.service.payment.PaymentCallbackService;
import com.gii.api.service.payment.PaymentWebhookService;
import com.gii.api.service.payment.PendingOrderService;
import com.gii.api.service.payment.ReceiptService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentApiController implements PaymentApi {

  private final PendingOrderService pendingOrderService;
  private final InitiatePaymentService initiatePaymentService;
  private final PaymentCallbackService paymentCallbackService;
  private final PaymentWebhookService paymentWebhookService;
  private final OrderStatusService orderStatusService;
  private final ReceiptService receiptService;

  @Override
  public ResponseEntity<CheckoutOrderResponse> createPendingOrder(
      UUID courseId, Authentication authentication) {
    return ResponseEntity.ok(pendingOrderService.execute(courseId, authentication));
  }

  @Override
  public ResponseEntity<PaymentStatusResponse> getOrderStatus(
      UUID orderId, Authentication authentication) {
    return ResponseEntity.ok(orderStatusService.execute(orderId, authentication));
  }

  @Override
  public ResponseEntity<PaymentInitiationResponse> initiatePayment(
      UUID orderId, InitiatePaymentRequest request, Authentication authentication) {
    return ResponseEntity.ok(initiatePaymentService.execute(orderId, request, authentication));
  }

  @Override
  public ResponseEntity<PaymentStatusResponse> paymentSuccess(
      UUID orderId, Map<String, String> queryParams) {
    return ResponseEntity.ok(paymentCallbackService.success(orderId, queryParams));
  }

  @Override
  public ResponseEntity<PaymentStatusResponse> paymentFailed(
      UUID orderId, Map<String, String> queryParams) {
    return ResponseEntity.ok(paymentCallbackService.failed(orderId, queryParams));
  }

  @Override
  public ResponseEntity<PaymentStatusResponse> paymentCancelled(
      UUID orderId, Map<String, String> queryParams) {
    return ResponseEntity.ok(paymentCallbackService.cancelled(orderId, queryParams));
  }

  @Override
  public ResponseEntity<WebhookAckResponse> sslcommerzWebhook(
      Map<String, String> headers, String payload) {
    return ResponseEntity.ok(paymentWebhookService.sslcommerz(headers, payload));
  }

  @Override
  public ResponseEntity<WebhookAckResponse> bkashWebhook(
      Map<String, String> headers, String payload) {
    return ResponseEntity.ok(paymentWebhookService.bkash(headers, payload));
  }

  @Override
  public ResponseEntity<WebhookAckResponse> nagadWebhook(
      Map<String, String> headers, String payload) {
    return ResponseEntity.ok(paymentWebhookService.nagad(headers, payload));
  }

  @Override
  public ResponseEntity<ReceiptResponse> getReceipt(UUID orderId, Authentication authentication) {
    return ResponseEntity.ok(receiptService.execute(orderId, authentication));
  }
}
