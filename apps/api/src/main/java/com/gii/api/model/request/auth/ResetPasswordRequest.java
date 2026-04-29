package com.gii.api.model.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ResetPasswordRequest(
        @NotBlank String token,  // Reset token from email/SMS
        @NotBlank String newPassword
) {}