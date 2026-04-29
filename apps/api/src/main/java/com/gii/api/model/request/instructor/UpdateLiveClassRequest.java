package com.gii.api.model.request.instructor;

import jakarta.validation.constraints.Future;
import lombok.Builder;

import java.time.Instant;

@Builder
public record UpdateLiveClassRequest(
        String title,  // Optional: update title
        String description,  // Optional: update description
        @Future Instant startsAt,  // Optional: update start time
        @Future Instant endsAt,  // Optional: update end time
        String status  // Optional: e.g., "SCHEDULED", "CANCELLED"
) {}