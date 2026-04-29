package com.gii.api.model.request.instructor;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record CreateLiveClassRequest(
        @NotNull UUID sectionId,  // Section where lesson belongs
        @NotNull UUID lessonId,  // Lesson this live class is for
        @NotBlank String title,
        String description,  // Optional
        @NotNull @Future Instant startsAt,
        @NotNull @Future Instant endsAt,
        String zoomMeetingLink  // Optional: pre-configured Zoom meeting URL or ID
) {}