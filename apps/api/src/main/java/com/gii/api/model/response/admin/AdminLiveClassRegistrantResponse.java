package com.gii.api.model.response.admin;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminLiveClassRegistrantResponse(
    UUID registrantId,
    UUID userId,
    String studentName,
    String studentEmail,
    String status, // PENDING, APPROVED, FAILED, CANCELLED
    String zoomRegistrantId,
    Boolean attended,
    Instant joinedAt,
    Instant leftAt,
    Integer durationSeconds) {}
