package com.gii.api.model.response.auth;

import com.gii.common.enums.VerificationChannel;
import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record RegisterResponse(
        UUID userId,
        VerificationChannel channel
) {}
