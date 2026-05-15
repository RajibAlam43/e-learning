package com.gii.api.model.response.live;

import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LiveClassUpsertResponse(
    UUID liveClassId,
    String title,
    String description,
    UUID courseId,
    String courseName,
    UUID sectionId,
    String sectionTitle,
    Instant startsAt,
    Instant endsAt,
    LiveClassProvider provider,
    LiveClassStatus status,
    String meetingId,
    String hostStartUrl,
    String joinUrl,
    Instant createdAt,
    Instant updatedAt) {}
