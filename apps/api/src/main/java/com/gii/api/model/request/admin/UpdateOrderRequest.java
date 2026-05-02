package com.gii.api.model.request.admin;

import lombok.Builder;

@Builder
public record UpdateOrderRequest(
    String status, // PENDING, PAID, FAILED, REFUNDED, CANCELLED
    String adminNote // Internal note for order
    ) {}
