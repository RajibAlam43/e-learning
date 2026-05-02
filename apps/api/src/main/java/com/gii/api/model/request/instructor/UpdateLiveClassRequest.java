package com.gii.api.model.request.instructor;

import com.gii.common.enums.LiveClassStatus;
import jakarta.validation.constraints.Future;
import java.time.Instant;
import lombok.Builder;

@Builder
public record UpdateLiveClassRequest(
    String title, // Optional: update title
    String description, // Optional: update description
    @Future Instant startsAt, // Optional: update start time
    @Future Instant endsAt, // Optional: update end time
    LiveClassStatus status // Optional: e.g., "SCHEDULED", "CANCELLED"
    ) {}
