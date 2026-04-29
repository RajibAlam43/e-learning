package com.gii.api.model.request.payment;

import com.gii.common.enums.OrderProvider;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record InitiatePaymentRequest(
        @NotNull OrderProvider provider,  // SSLCOMMERZ, BKASH, NAGAD
        String customerPhone,  // Optional but recommended for bKash/Nagad
        String customerEmail,  // Optional
        String paymentMethod,  // Optional: specific method (e.g., "bkash_app", "nagad_web")
        Boolean savePaymentMethod  // Optional: save for future transactions
) {}