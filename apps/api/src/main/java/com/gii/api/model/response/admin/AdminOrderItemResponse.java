package com.gii.api.model.response.admin;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminOrderItemResponse(
    UUID courseId,
    String courseName,
    BigDecimal priceBdt,
    BigDecimal discountBdt,
    BigDecimal finalAmount) {}
