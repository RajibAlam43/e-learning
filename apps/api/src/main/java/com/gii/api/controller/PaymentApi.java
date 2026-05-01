package com.gii.api.controller;

import com.gii.api.model.request.payment.InitiatePaymentRequest;
import com.gii.api.model.response.payment.CheckoutOrderResponse;
import com.gii.api.model.response.payment.PaymentInitiationResponse;
import com.gii.api.model.response.payment.PaymentStatusResponse;
import com.gii.api.model.response.payment.ReceiptResponse;
import com.gii.api.model.response.payment.WebhookAckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Payments", description = "Course checkout, payment initiation, and webhook handling")
@PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
public interface PaymentApi {

  @PostMapping("/checkout/courses/{courseId}")
  @Operation(
      summary = "Create pending order",
      description = "Create a pending checkout order for a course.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order created",
            content = @Content(schema = @Schema(implementation = CheckoutOrderResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Course not found"),
        @ApiResponse(responseCode = "409", description = "Already enrolled")
      })
  ResponseEntity<CheckoutOrderResponse> createPendingOrder(
      @PathVariable UUID courseId, Authentication authentication);

  @GetMapping("/checkout/orders/{orderId}")
  @Operation(
      summary = "Get order status",
      description = "Check payment status of an order.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order status retrieved",
            content = @Content(schema = @Schema(implementation = PaymentStatusResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Order not found")
      })
  ResponseEntity<PaymentStatusResponse> getOrderStatus(
      @PathVariable UUID orderId, Authentication authentication);

  @PostMapping("/payments/{orderId}/initiate")
  @Operation(
      summary = "Initiate payment",
      description = "Start payment process with selected provider (SSLCommerz, bKash, Nagad).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment initiated",
            content = @Content(schema = @Schema(implementation = PaymentInitiationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid provider or order"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Order not found")
      })
  ResponseEntity<PaymentInitiationResponse> initiatePayment(
      @PathVariable UUID orderId,
      @RequestBody InitiatePaymentRequest request,
      Authentication authentication);

  @GetMapping("/payments/{orderId}/success")
  @Operation(
      summary = "Payment success callback",
      description = "Handle payment provider success redirect.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment processed",
            content = @Content(schema = @Schema(implementation = PaymentStatusResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid callback data"),
        @ApiResponse(responseCode = "404", description = "Order not found")
      })
  ResponseEntity<PaymentStatusResponse> paymentSuccess(
      @PathVariable UUID orderId, @RequestParam Map<String, String> queryParams);

  @GetMapping("/payments/{orderId}/failed")
  @Operation(
      summary = "Payment failed callback",
      description = "Handle payment provider failed redirect.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Failure recorded",
            content = @Content(schema = @Schema(implementation = PaymentStatusResponse.class))),
        @ApiResponse(responseCode = "404", description = "Order not found")
      })
  ResponseEntity<PaymentStatusResponse> paymentFailed(
      @PathVariable UUID orderId, @RequestParam Map<String, String> queryParams);

  @GetMapping("/payments/{orderId}/cancelled")
  @Operation(
      summary = "Payment cancelled callback",
      description = "Handle payment provider cancelled redirect.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cancellation recorded",
            content = @Content(schema = @Schema(implementation = PaymentStatusResponse.class))),
        @ApiResponse(responseCode = "404", description = "Order not found")
      })
  ResponseEntity<PaymentStatusResponse> paymentCancelled(
      @PathVariable UUID orderId, @RequestParam Map<String, String> queryParams);

  @PostMapping("/public/webhooks/payments/sslcommerz")
  @Operation(
      summary = "SSLCommerz webhook",
      description = "Receive and process SSLCommerz webhook notifications.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Webhook acknowledged",
            content = @Content(schema = @Schema(implementation = WebhookAckResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid webhook signature")
      })
  ResponseEntity<WebhookAckResponse> sslcommerzWebhook(
      @RequestHeader Map<String, String> headers, @RequestBody String payload);

  @PostMapping("/public/webhooks/payments/bkash")
  @Operation(
      summary = "bKash webhook",
      description = "Receive and process bKash webhook notifications.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Webhook acknowledged",
            content = @Content(schema = @Schema(implementation = WebhookAckResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid webhook signature")
      })
  ResponseEntity<WebhookAckResponse> bkashWebhook(
      @RequestHeader Map<String, String> headers, @RequestBody String payload);

  @PostMapping("/public/webhooks/payments/nagad")
  @Operation(
      summary = "Nagad webhook",
      description = "Receive and process Nagad webhook notifications.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Webhook acknowledged",
            content = @Content(schema = @Schema(implementation = WebhookAckResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid webhook signature")
      })
  ResponseEntity<WebhookAckResponse> nagadWebhook(
      @RequestHeader Map<String, String> headers, @RequestBody String payload);

  @GetMapping("/student/orders/{orderId}/receipt")
  @Operation(
      summary = "Get order receipt",
      description = "Retrieve receipt for a paid order.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Receipt retrieved",
            content = @Content(schema = @Schema(implementation = ReceiptResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not order owner"),
        @ApiResponse(responseCode = "404", description = "Order not found")
      })
  ResponseEntity<ReceiptResponse> getReceipt(
      @PathVariable UUID orderId, Authentication authentication);
}
