package com.gii.api.model.response.auth;

import com.gii.common.enums.VerificationChannel;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AuthResponse(
    String accessToken, // JWT access token
    boolean isVerified,
    VerificationChannel channel,
    UUID userId, // Optional: User ID for client-side state
    String fullName, // Optional: User's full name
    Set<String> roles // Optional: User's roles (e.g., STUDENT, INSTRUCTOR)
) {}
