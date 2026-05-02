package com.gii.api.model.response.payment;

import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PaymentStatusResponse(
    UUID orderId,

    // Order details
    OrderStatus status, // PENDING, PAID, FAILED, REFUNDED, CANCELLED
    BigDecimal totalAmount,
    String currency,

    // Payment details
    OrderProvider provider, // Payment provider used
    String providerTransactionId, // Transaction ID from provider

    // Timing
    Instant createdAt,
    Instant paidAt, // When payment was successful
    Instant refundedAt, // If refunded

    // Customer
    String customerEmail,
    String customerPhone,

    // Enrollment status
    Boolean coursesEnrolled, // Whether student got course access
    Integer enrolledCourseCount,

    // Next action for client
    String nextAction, // e.g., "REDIRECT_TO_DASHBOARD", "INITIATE_PAYMENT", "RETRY_PAYMENT"
    String actionUrl, // URL for next action (if any)

    // Message
    String message // e.g., "Payment successful!", "Payment pending...", "Payment failed"
) {}
