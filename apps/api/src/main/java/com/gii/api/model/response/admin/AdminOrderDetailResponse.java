package com.gii.api.model.response.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminOrderDetailResponse(
    UUID orderId,
    UUID userId,
    String customerName,
    String customerEmail,
    String customerPhone,
    BigDecimal totalAmount,
    String currency,
    String provider,
    String providerTxnId,
    String status,
    Instant paidAt,
    Instant refundedAt,
    Instant createdAt,
    Instant updatedAt,
    String adminNote,
    List<AdminOrderItemResponse> items) {}
