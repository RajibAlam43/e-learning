package com.gii.api.controller;

import com.gii.api.model.request.payment.CreateCheckoutOrderRequest;
import com.gii.api.model.request.payment.InitiatePaymentRequest;
import com.gii.api.model.response.payment.CheckoutOrderResponse;
import com.gii.api.model.response.payment.PaymentInitiationResponse;
import com.gii.api.model.response.payment.PaymentStatusResponse;
import com.gii.api.model.response.payment.ReceiptResponse;
import com.gii.api.model.response.payment.WebhookAckResponse;
import com.gii.api.service.payment.InitiatePaymentService;
import com.gii.api.service.payment.OrderStatusService;
import com.gii.api.service.payment.PendingCartOrderService;
import com.gii.api.service.payment.callback.PaymentCallbackService;
import com.gii.api.service.payment.webhook.PaymentWebhookService;
import com.gii.api.service.payment.ReceiptService;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class PaymentApiController implements PaymentApi {

  private final PendingCartOrderService pendingCartOrderService;
  private final InitiatePaymentService initiatePaymentService;
  private final PaymentCallbackService paymentCallbackService;
  private final PaymentWebhookService paymentWebhookService;
  private final OrderStatusService orderStatusService;
  private final ReceiptService receiptService;

  @Value("${payments.frontend-base-url}")
  private String paymentsFrontendBaseUrl;

  @Override
  public ResponseEntity<CheckoutOrderResponse> createPendingCartOrder(
      CreateCheckoutOrderRequest request, Authentication authentication) {
    return ResponseEntity.ok(pendingCartOrderService.execute(request, authentication));
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
  public ResponseEntity<Void> paymentSuccess(
      UUID orderId, Map<String, String> queryParams) {
    paymentCallbackService.success(orderId, queryParams);
    return ResponseEntity.status(303)
        .location(buildRedirectUri(orderId, "success"))
        .build();
  }

  @Override
  public ResponseEntity<Void> paymentFailed(
      UUID orderId, Map<String, String> queryParams) {
    paymentCallbackService.failed(orderId, queryParams);
    return ResponseEntity.status(303)
        .location(buildRedirectUri(orderId, "failed"))
        .build();
  }

  @Override
  public ResponseEntity<Void> paymentCancelled(
      UUID orderId, Map<String, String> queryParams) {
    paymentCallbackService.cancelled(orderId, queryParams);
    return ResponseEntity.status(303)
        .location(buildRedirectUri(orderId, "cancelled"))
        .build();
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
  public ResponseEntity<ReceiptResponse> getReceipt(UUID orderId, Authentication authentication) {
    return ResponseEntity.ok(receiptService.execute(orderId, authentication));
  }

  private URI buildRedirectUri(UUID orderId, String status) {
    return UriComponentsBuilder.fromUriString(paymentsFrontendBaseUrl)
        .queryParam("orderId", orderId)
        .queryParam("status", status)
        .build()
        .toUri();
  }
}
