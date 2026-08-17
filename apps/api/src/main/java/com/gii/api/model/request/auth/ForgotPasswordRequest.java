package com.gii.api.model.request.auth;

import com.gii.common.enums.VerificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ForgotPasswordRequest(
    @NotNull VerificationChannel channel, // EMAIL or PHONE
    @NotBlank String identifier // Email or phone
    ) {}
