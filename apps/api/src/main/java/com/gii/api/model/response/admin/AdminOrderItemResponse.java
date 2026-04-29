package com.gii.api.model.response.admin;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record AdminOrderItemResponse(
        UUID courseId,
        String courseName,
        BigDecimal priceBdt,
        BigDecimal discountBdt,
        BigDecimal finalAmount
) {}