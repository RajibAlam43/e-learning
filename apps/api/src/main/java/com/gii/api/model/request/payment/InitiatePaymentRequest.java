package com.gii.api.model.request.payment;

import com.gii.common.enums.OrderProvider;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record InitiatePaymentRequest(
    @NotNull OrderProvider provider // SSLCOMMERZ, BKASH, NAGAD
) {}
