package com.gii.api.model.response.payment;

import com.gii.common.enums.OrderProvider;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PaymentInitiationResponse(
    UUID orderId,
    OrderProvider provider, // SSLCOMMERZ, BKASH, NAGAD
    String sessionId, // Provider session/transaction ID

    // Gateway redirect information
    String redirectUrl, // URL to redirect user to payment gateway
    String gatewayName, // Display name of gateway

    // Alternative: embedded payment link
    String paymentUrl, // Direct payment link (without redirect)

    // Timeout info
    Long timeoutSeconds, // Payment session expires in X seconds

    // Provider-specific metadata
    String providerTransactionId, // Preliminary transaction ID from provider
    String providerReference, // Reference for provider communication

    // Client-side hints
    String successCallbackUrl, // Where to return after success
    String failureCallbackUrl // Where to return after failure
) {}
