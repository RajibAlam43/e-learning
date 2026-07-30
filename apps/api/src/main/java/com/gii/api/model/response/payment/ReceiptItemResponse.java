package com.gii.api.model.response.payment;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ReceiptItemResponse(
    UUID itemId,
    String itemName,
    String itemSlug,
    UUID courseId,
    String courseName,
    String courseSlug,

    // Pricing details
    BigDecimal unitPrice,
    BigDecimal discountAmount,
    BigDecimal lineTotal, // After discount

    // Access info
    String accessStartDate,
    String accessExpiryDate // If applicable
) {}
