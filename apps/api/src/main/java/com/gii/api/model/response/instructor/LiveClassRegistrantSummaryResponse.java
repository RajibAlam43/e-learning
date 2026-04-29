package com.gii.api.model.response.instructor;

import com.gii.common.enums.LiveClassRegistrantStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record LiveClassRegistrantSummaryResponse(
        UUID registrantId,
        UUID userId,
        String studentName,
        String studentEmail,
        
        // Registration status
        LiveClassRegistrantStatus status,  // PENDING, APPROVED, FAILED, CANCELLED
        String zoomRegistrantId,
        
        // Attendance (populated after class)
        Boolean attended,
        Instant joinedAt,
        Instant leftAt,
        Integer durationSeconds,  // Actual attendance duration
        
        // Registration timestamp
        Instant registeredAt
) {}