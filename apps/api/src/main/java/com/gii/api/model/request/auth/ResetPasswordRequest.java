package com.gii.api.model.request.auth;

import com.gii.common.enums.VerificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ResetPasswordRequest(
        @NotNull VerificationChannel channel,
        @NotBlank String identifier,
        @NotBlank String code,
        @NotBlank String newPassword
) {}
