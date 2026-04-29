package com.gii.api.model.request.admin;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record CreateLiveClassRequest(
        @NotNull UUID sectionId,
        @NotNull UUID lessonId,
        @NotBlank String title,
        String description,
        @NotNull @Future Instant startsAt,
        @NotNull @Future Instant endsAt,
        String zoomMeetingLink
) {}