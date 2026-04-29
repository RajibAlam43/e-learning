package com.gii.api.model.response.admin;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AdminLiveClassSummaryResponse(
        UUID liveClassId,
        String title,
        String courseName,
        String instructorName,
        String status,  // SCHEDULED, LIVE, COMPLETED, CANCELLED, FAILED
        Instant startsAt,
        Integer registeredStudents,
        Instant createdAt
) {}