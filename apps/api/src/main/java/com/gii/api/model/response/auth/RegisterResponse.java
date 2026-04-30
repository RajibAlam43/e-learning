package com.gii.api.model.response.auth;

import com.gii.common.enums.VerificationChannel;
import java.util.UUID;
import lombok.Builder;

@Builder
public record RegisterResponse(UUID userId, VerificationChannel channel) {}
