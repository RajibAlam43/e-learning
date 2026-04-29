package com.gii.api.model.response.admin;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AdminLiveClassRegistrantResponse(
        UUID registrantId,
        UUID userId,
        String studentName,
        String studentEmail,
        String status,  // PENDING, APPROVED, FAILED, CANCELLED
        String zoomRegistrantId,
        Boolean attended,
        Instant joinedAt,
        Instant leftAt,
        Integer durationSeconds
) {}