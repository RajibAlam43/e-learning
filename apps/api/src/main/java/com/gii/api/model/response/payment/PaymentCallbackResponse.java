package com.gii.api.model.response.payment;

import java.util.Map;
import lombok.Builder;

@Builder
public record PaymentCallbackResponse(
    PaymentStatusResponse paymentStatus,
    Map<String, String> providerMetadata, // Raw provider callback data
    String callbackType // "SUCCESS", "FAILED", "CANCELLED"
) {}
