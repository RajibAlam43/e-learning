package com.gii.common.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record SslcommerzValidationJobMessage(
    UUID orderId,
    String providerTxnId,
    String valId,
    String source,
    int attempt,
    int maxAttempts,
    Instant createdAt) {}
