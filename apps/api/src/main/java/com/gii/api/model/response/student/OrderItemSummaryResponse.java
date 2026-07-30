package com.gii.api.model.response.student;

import com.gii.common.enums.OrderItemType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record OrderItemSummaryResponse(
    OrderItemType itemType,
    UUID courseId,
    UUID collectionId,
    String courseName,
    BigDecimal priceBdt,
    BigDecimal discountBdt, // Discount applied
    BigDecimal finalAmount // Price - discount
) {}
