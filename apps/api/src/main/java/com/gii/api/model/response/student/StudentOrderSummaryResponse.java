package com.gii.api.model.response.student;

import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentOrderSummaryResponse(
    UUID orderId,

    // Order details
    OrderStatus status, // PENDING, PAID, FAILED, REFUNDED, CANCELLED
    BigDecimal totalAmount, // Amount in BDT
    String currency, // BDT
    OrderProvider provider, // SSLCOMMERZ, BKASH

    // Courses purchased
    Integer courseCount,
    List<OrderItemSummaryResponse> items,

    // Timestamps
    Instant createdAt,
    Instant paidAt, // Null if not paid
    Instant refundedAt // Null if not refunded
    ) {}
