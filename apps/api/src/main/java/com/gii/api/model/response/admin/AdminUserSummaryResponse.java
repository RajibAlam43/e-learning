package com.gii.api.model.response.admin;

import com.gii.common.enums.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminUserSummaryResponse(
    UUID userId,
    String fullName,
    String email,
    String phone,
    String phoneCountryCode,
    List<String> roles,
    UserStatus status,
    Instant createdAt) {}
