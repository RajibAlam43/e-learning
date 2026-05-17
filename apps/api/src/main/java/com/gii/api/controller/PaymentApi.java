package com.gii.api.controller;

import com.gii.api.model.request.payment.CreateCheckoutOrderRequest;
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
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Payments", description = "Cart checkout, payment initiation, and webhook handling")
public interface PaymentApi {

  @PostMapping("/checkout/orders")
  @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
  @Operation(
      summary = "Create pending cart order",
      description = "Create a pending checkout order for mixed COURSE/COLLECTION cart items.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order created",
            content = @Content(schema = @Schema(implementation = CheckoutOrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid cart payload"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Course/collection not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate purchase conflict")
      })
  ResponseEntity<CheckoutOrderResponse> createPendingCartOrder(
      @RequestBody @Valid CreateCheckoutOrderRequest request, Authentication authentication);

  @GetMapping("/checkout/orders/{orderId}")
  @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
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
  @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
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
      @RequestBody @Valid InitiatePaymentRequest request,
      Authentication authentication);

  @RequestMapping(
          value = "/payments/sslcommerz/{orderId}/success",
          method = {RequestMethod.GET, RequestMethod.POST}
  )
  @Operation(
      summary = "Payment success callback",
      description = "Handle payment provider success redirect.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "303", description = "Redirect to frontend result page"),
        @ApiResponse(responseCode = "400", description = "Invalid callback data"),
        @ApiResponse(responseCode = "404", description = "Order not found")
      })
  ResponseEntity<Void> sslcommerzPaymentSuccess(
      @PathVariable UUID orderId, @RequestParam Map<String, String> queryParams);

  @RequestMapping(
          value = "/payments/sslcommerz/{orderId}/failed",
          method = {RequestMethod.GET, RequestMethod.POST}
  )
  @Operation(
      summary = "Payment failed callback",
      description = "Handle payment provider failed redirect.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "303", description = "Redirect to frontend result page"),
        @ApiResponse(responseCode = "404", description = "Order not found")
      })
  ResponseEntity<Void> sslcommerzPaymentFailed(
      @PathVariable UUID orderId, @RequestParam Map<String, String> queryParams);

  @RequestMapping(
          value = "/payments/sslcommerz/{orderId}/cancelled",
          method = {RequestMethod.GET, RequestMethod.POST}
  )
  @Operation(
      summary = "Payment cancelled callback",
      description = "Handle payment provider cancelled redirect.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "303", description = "Redirect to frontend result page"),
        @ApiResponse(responseCode = "404", description = "Order not found")
      })
  ResponseEntity<Void> sslcommerzPaymentCancelled(
      @PathVariable UUID orderId, @RequestParam Map<String, String> queryParams);

  @RequestMapping(
          value = "/payments/bkash/{orderId}/success",
          method = {RequestMethod.GET, RequestMethod.POST}
  )
  @Operation(
      summary = "bKash payment success callback",
      description = "Handle bKash success redirect.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "303", description = "Redirect to frontend result page"),
        @ApiResponse(responseCode = "400", description = "Invalid callback data"),
        @ApiResponse(responseCode = "404", description = "Order not found")
      })
  ResponseEntity<Void> bkashPaymentSuccess(
      @PathVariable UUID orderId, @RequestParam Map<String, String> queryParams);

  @RequestMapping(
          value = "/payments/bkash/{orderId}/failed",
          method = {RequestMethod.GET, RequestMethod.POST}
  )
  @Operation(
      summary = "bKash payment failed callback",
      description = "Handle bKash failed redirect.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "303", description = "Redirect to frontend result page"),
        @ApiResponse(responseCode = "404", description = "Order not found")
      })
  ResponseEntity<Void> bkashPaymentFailed(
      @PathVariable UUID orderId, @RequestParam Map<String, String> queryParams);

  @RequestMapping(
          value = "/payments/bkash/{orderId}/cancelled",
          method = {RequestMethod.GET, RequestMethod.POST}
  )
  @Operation(
      summary = "bKash payment cancelled callback",
      description = "Handle bKash cancelled redirect.",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "303", description = "Redirect to frontend result page"),
        @ApiResponse(responseCode = "404", description = "Order not found")
      })
  ResponseEntity<Void> bkashPaymentCancelled(
      @PathVariable UUID orderId, @RequestParam Map<String, String> queryParams);

  @PostMapping(
      value = "/public/webhooks/payments/sslcommerz",
      consumes = "application/x-www-form-urlencoded")
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
      @RequestHeader Map<String, String> headers, @RequestParam Map<String, String> params);

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

  @GetMapping("/student/orders/{orderId}/receipt")
  @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
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
