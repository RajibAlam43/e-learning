package com.gii.api.model.request;

import lombok.Builder;

@Builder
public record ResendVerificationRequest(
        String email
) {}
