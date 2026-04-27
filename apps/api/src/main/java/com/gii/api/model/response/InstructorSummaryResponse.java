package com.gii.api.model.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record InstructorSummaryResponse(
        UUID id,
        String fullName,
        String avatarUrl,
        String shortBio,
        String credentials
) {}
