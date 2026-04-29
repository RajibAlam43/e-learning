package com.gii.api.model.response.auth;

import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record AuthResponse(
        String accessToken,  // JWT access token
        UUID userId,  // Optional: User ID for client-side state
        String fullName,  // Optional: User's full name
        Set<String> roles  // Optional: User's roles (e.g., STUDENT, INSTRUCTOR)
) {}