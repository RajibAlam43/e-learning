package com.gii.api.controller;

import com.gii.api.model.request.payment.InitiatePaymentRequest;
import com.gii.api.model.response.payment.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentApiController implements PaymentApi {

    @Override
    public ResponseEntity<CheckoutOrderResponse> createPendingOrder(UUID courseId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<PaymentStatusResponse> getOrderStatus(UUID orderId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(UUID orderId, InitiatePaymentRequest request, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<PaymentStatusResponse> paymentSuccess(UUID orderId, Map<String, String> queryParams) {
        return null;
    }

    @Override
    public ResponseEntity<PaymentStatusResponse> paymentFailed(UUID orderId, Map<String, String> queryParams) {
        return null;
    }

    @Override
    public ResponseEntity<PaymentStatusResponse> paymentCancelled(UUID orderId, Map<String, String> queryParams) {
        return null;
    }

    @Override
    public ResponseEntity<WebhookAckResponse> sslcommerzWebhook(Map<String, String> headers, String payload) {
        return null;
    }

    @Override
    public ResponseEntity<WebhookAckResponse> bkashWebhook(Map<String, String> headers, String payload) {
        return null;
    }

    @Override
    public ResponseEntity<WebhookAckResponse> nagadWebhook(Map<String, String> headers, String payload) {
        return null;
    }

    @Override
    public ResponseEntity<ReceiptResponse> getReceipt(UUID orderId, Authentication authentication) {
        return null;
    }
}
