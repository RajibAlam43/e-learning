package com.gii.api.model.request.instructor;

import com.gii.common.enums.LiveClassProvider;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateLiveClassRequest(
    @NotNull UUID sectionId, // Section where lesson belongs
    @NotNull UUID lessonId, // Lesson this live class is for
    @NotBlank String title,
    String description, // Optional
    @NotNull @Future Instant startsAt,
    @NotNull @Future Instant endsAt,
    LiveClassProvider provider, // Optional: defaults to ZOOM for legacy compatibility
    String providerMeetingId, // Optional: provider meeting identifier
    String hostStartUrl, // Optional: host/instructor start URL
    String participantJoinUrl, // Optional: participant join URL
    String zoomMeetingLink // Legacy alias fallback for participantJoinUrl
    ) {}
