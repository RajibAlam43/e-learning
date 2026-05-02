package com.gii.api.model.response.student;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record OrderItemSummaryResponse(
    UUID courseId,
    String courseName,
    BigDecimal priceBdt,
    BigDecimal discountBdt, // Discount applied
    BigDecimal finalAmount // Price - discount
) {}
