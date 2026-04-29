// (Used internally; extends PaymentStatusResponse with provider-specific details)
package com.gii.api.model.response.payment;

import lombok.Builder;

import java.util.Map;

@Builder
public record PaymentCallbackResponse(
        PaymentStatusResponse paymentStatus,
        Map<String, String> providerMetadata,  // Raw provider callback data
        String callbackType  // "SUCCESS", "FAILED", "CANCELLED"
) {}