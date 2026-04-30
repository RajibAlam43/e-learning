package com.gii.api.model.request.auth;

import com.gii.common.enums.VerificationChannel;
import com.gii.common.enums.VerificationPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ResendVerificationRequest(
        @NotNull VerificationChannel channel,  // EMAIL or PHONE
        @NotBlank String identifier,  // Email or phone
        @NotNull VerificationPurpose purpose
) {}
