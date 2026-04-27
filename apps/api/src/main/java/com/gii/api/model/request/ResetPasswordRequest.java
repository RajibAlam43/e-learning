package com.gii.api.model.request;

import lombok.Builder;

@Builder
public record ResetPasswordRequest(
        String token,
        String newPassword
) {}
