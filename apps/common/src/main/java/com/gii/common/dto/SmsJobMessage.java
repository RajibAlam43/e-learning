package com.gii.common.dto;

import com.gii.common.enums.VerificationPurpose;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record SmsJobMessage(
    UUID userId,
    String toPhoneNumber,
    String message,
    VerificationPurpose verificationPurpose,
    String verificationCode,
    Instant createdAt) {}
