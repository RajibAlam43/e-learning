package com.gii.api.model.response.payment;

import com.gii.common.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CheckoutOrderResponse(
    UUID orderId,

    // Order totals
    BigDecimal subtotal, // Sum of item prices before discount
    BigDecimal totalDiscount, // Total discount amount
    BigDecimal totalAmount, // Final amount: subtotal - discount
    String currency, // BDT

    // Order items (courses)
    List<CheckoutOrderItemResponse> items,

    // Status
    OrderStatus status, // PENDING, PAID, FAILED, etc.

    // Expiration
    Instant expiresAt, // When unpaid order expires
    Boolean isExpired,

    // Customer info
    String customerEmail,
    String customerPhone,

    // Next action
    String nextAction // e.g., "INITIATE_PAYMENT"
) {}
