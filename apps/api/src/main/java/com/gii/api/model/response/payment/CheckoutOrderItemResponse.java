package com.gii.api.model.response.payment;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record CheckoutOrderItemResponse(
        UUID courseId,
        String courseName,
        String courseSlug,
        String courseThumbnailUrl,
        
        // Pricing
        BigDecimal originalPrice,
        BigDecimal discountAmount,
        BigDecimal finalPrice,  // originalPrice - discountAmount
        
        // Discount reason (if any)
        String discountReason  // e.g., "PROMOTIONAL_CODE", "BULK_PURCHASE", "EARLY_BIRD"
) {}