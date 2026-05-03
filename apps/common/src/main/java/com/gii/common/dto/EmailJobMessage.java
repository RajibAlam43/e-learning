package com.gii.common.dto;

import com.gii.common.enums.EmailJobType;
import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record EmailJobMessage(
    UUID userId,
    EmailJobType jobType,
    String toEmail,
    String subject,
    String body,
    VerificationPurpose verificationPurpose,
    VerificationChannel verificationChannel,
    String verificationCode,
    Instant createdAt) {}

