package com.gii.api.model.response.admin;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record AdminLiveClassDetailResponse(
        UUID liveClassId,
        String title,
        String description,
        UUID courseId,
        String courseName,
        UUID sectionId,
        String sectionTitle,
        UUID lessonId,
        String lessonTitle,
        UUID instructorId,
        String instructorName,
        Instant startsAt,
        Instant endsAt,
        String status,
        String zoomMeetingId,
        String zoomStartUrl,
        String zoomJoinUrl,
        Instant createdAt,
        Instant updatedAt,
        List<AdminLiveClassRegistrantResponse> registrants
) {}