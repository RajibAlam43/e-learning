package com.gii.api.model.response.admin;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record AdminOrderSummaryResponse(
        UUID orderId,
        String customerName,
        String customerEmail,
        BigDecimal totalAmount,
        String status,  // PENDING, PAID, FAILED, REFUNDED, CANCELLED
        String provider,  // SSLCOMMERZ, BKASH
        Instant createdAt,
        Instant paidAt
) {}