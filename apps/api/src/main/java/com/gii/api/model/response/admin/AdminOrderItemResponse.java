package com.gii.api.model.response.admin;

import com.gii.common.enums.OrderItemType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminOrderItemResponse(
    OrderItemType itemType,
    UUID courseId,
    UUID collectionId,
    String courseName,
    BigDecimal priceBdt,
    BigDecimal discountBdt,
    BigDecimal finalAmount) {}
