package com.gii.api.model.response.admin;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AdminInstructorSummaryResponse(
        UUID userId,
        String fullName,
        String email,
        String displayName,
        String headline,
        Boolean isPublic,
        Integer assignedCoursesCount,
        Instant createdAt
) {}