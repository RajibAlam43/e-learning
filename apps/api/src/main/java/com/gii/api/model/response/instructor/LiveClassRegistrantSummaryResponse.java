package com.gii.api.model.response.instructor;

import com.gii.common.enums.LiveClassRegistrantStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LiveClassRegistrantSummaryResponse(
    UUID registrantId,
    UUID userId,
    String studentName,
    String studentEmail,

    // Registration status
    LiveClassRegistrantStatus status, // PENDING, APPROVED, FAILED, CANCELLED
    String zoomRegistrantId,

    // Attendance (populated after class)
    Boolean attended,
    Instant joinedAt,
    Instant leftAt,
    Integer durationSeconds, // Actual attendance duration

    // Registration timestamp
    Instant registeredAt) {}
