package com.gii.api.controller;

import com.gii.api.model.request.InitiatePaymentRequest;
import com.gii.api.model.response.CheckoutOrderResponse;
import com.gii.api.model.response.PaymentInitiationResponse;
import com.gii.api.model.response.PaymentStatusResponse;
import com.gii.api.model.response.ReceiptResponse;
import com.gii.api.model.response.WebhookAckResponse;
import com.gii.api.processor.PaymentApiProcessingService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentApiProcessingService paymentApiProcessingService;

    /**
     * Creates a pending checkout order for a course.
     *
     * @param courseId ID of the course to purchase
     * @param authentication authenticated user context from Spring Security
     * @return created pending order payload
     */
    @PostMapping("/checkout/courses/{courseId}")
    public ResponseEntity<@NotNull CheckoutOrderResponse> createPendingOrder(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(paymentApiProcessingService.createPendingOrder(courseId, authentication));
    }

    /**
     * Returns checkout order and payment status.
     *
     * @param orderId ID of the order
     * @param authentication authenticated user context from Spring Security
     * @return order/payment status payload
     */
    @GetMapping("/checkout/orders/{orderId}")
    public ResponseEntity<@NotNull PaymentStatusResponse> getOrderStatus(
            @PathVariable UUID orderId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(paymentApiProcessingService.getOrderStatus(orderId, authentication));
    }

    /**
     * Starts payment with selected provider (for example bKash/SSLCommerz/Nagad).
     *
     * @param orderId ID of the order to pay
     * @param request payment initiation input (provider/method metadata)
     * @param authentication authenticated user context from Spring Security
     * @return payment initiation payload (gateway redirect URL/session info)
     */
    @PostMapping("/payments/{orderId}/initiate")
    public ResponseEntity<@NotNull PaymentInitiationResponse> initiatePayment(
            @PathVariable UUID orderId,
            @RequestBody InitiatePaymentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(paymentApiProcessingService.initiatePayment(orderId, request, authentication));
    }

    /**
     * Handles provider success redirect/callback for a payment order.
     *
     * @param orderId ID of the order
     * @param queryParams raw query parameters provided by payment gateway
     * @return normalized payment status after success callback handling
     */
    @GetMapping("/payments/{orderId}/success")
    public ResponseEntity<@NotNull PaymentStatusResponse> paymentSuccess(
            @PathVariable UUID orderId,
            @RequestParam Map<String, String> queryParams
    ) {
        return ResponseEntity.ok(paymentApiProcessingService.handleSuccessCallback(orderId, queryParams));
    }

    /**
     * Handles provider failed redirect/callback for a payment order.
     *
     * @param orderId ID of the order
     * @param queryParams raw query parameters provided by payment gateway
     * @return normalized payment status after failed callback handling
     */
    @GetMapping("/payments/{orderId}/failed")
    public ResponseEntity<@NotNull PaymentStatusResponse> paymentFailed(
            @PathVariable UUID orderId,
            @RequestParam Map<String, String> queryParams
    ) {
        return ResponseEntity.ok(paymentApiProcessingService.handleFailedCallback(orderId, queryParams));
    }

    /**
     * Handles provider cancelled redirect/callback for a payment order.
     *
     * @param orderId ID of the order
     * @param queryParams raw query parameters provided by payment gateway
     * @return normalized payment status after cancel callback handling
     */
    @GetMapping("/payments/{orderId}/cancelled")
    public ResponseEntity<@NotNull PaymentStatusResponse> paymentCancelled(
            @PathVariable UUID orderId,
            @RequestParam Map<String, String> queryParams
    ) {
        return ResponseEntity.ok(paymentApiProcessingService.handleCancelledCallback(orderId, queryParams));
    }

    /**
     * Receives SSLCommerz webhook/IPN notifications.
     *
     * @param headers request headers from provider callback
     * @param payload raw webhook payload
     * @return acknowledgement payload
     */
    @PostMapping("/public/webhooks/payments/sslcommerz")
    public ResponseEntity<@NotNull WebhookAckResponse> sslcommerzWebhook(
            @RequestHeader Map<String, String> headers,
            @RequestBody String payload
    ) {
        return ResponseEntity.ok(paymentApiProcessingService.handleSslcommerzWebhook(headers, payload));
    }

    /**
     * Receives bKash webhook/callback notifications.
     *
     * @param headers request headers from provider callback
     * @param payload raw webhook payload
     * @return acknowledgement payload
     */
    @PostMapping("/public/webhooks/payments/bkash")
    public ResponseEntity<@NotNull WebhookAckResponse> bkashWebhook(
            @RequestHeader Map<String, String> headers,
            @RequestBody String payload
    ) {
        return ResponseEntity.ok(paymentApiProcessingService.handleBkashWebhook(headers, payload));
    }

    /**
     * Receives Nagad webhook/callback notifications.
     *
     * @param headers request headers from provider callback
     * @param payload raw webhook payload
     * @return acknowledgement payload
     */
    @PostMapping("/public/webhooks/payments/nagad")
    public ResponseEntity<@NotNull WebhookAckResponse> nagadWebhook(
            @RequestHeader Map<String, String> headers,
            @RequestBody String payload
    ) {
        return ResponseEntity.ok(paymentApiProcessingService.handleNagadWebhook(headers, payload));
    }

    /**
     * Returns receipt metadata or download URL for a student's order.
     *
     * @param orderId ID of the order
     * @param authentication authenticated user context from Spring Security
     * @return receipt payload for the given order
     */
    @GetMapping("/student/orders/{orderId}/receipt")
    public ResponseEntity<@NotNull ReceiptResponse> getReceipt(
            @PathVariable UUID orderId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(paymentApiProcessingService.getReceipt(orderId, authentication));
    }
}
