package com.gii.api.model.response.payment;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ReceiptItemResponse(
        UUID courseId,
        String courseName,
        String courseSlug,
        
        // Pricing details
        BigDecimal unitPrice,
        BigDecimal discountAmount,
        BigDecimal lineTotal,  // After discount
        
        // Access info
        String accessStartDate,
        String accessExpiryDate  // If applicable
) {}