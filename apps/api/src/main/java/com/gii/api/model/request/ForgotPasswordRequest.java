package com.gii.api.model.request;

import lombok.Builder;

@Builder
public record ForgotPasswordRequest(
        String email
) {}
