package com.gii.api.model.response.admin;

import com.gii.common.enums.LiveClassProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminLiveClassDetailResponse(
    UUID liveClassId,
    String title,
    String titleEn,
    String description,
    String descriptionEn,
    UUID courseId,
    String courseName,
    String courseNameEn,
    UUID sectionId,
    String sectionTitle,
    String sectionTitleEn,
    Integer position,
    UUID instructorId,
    String instructorName,
    Instant startsAt,
    Instant endsAt,
    LiveClassProvider provider,
    String status,
    String meetingId,
    String hostStartUrl,
    String joinUrl,
    Instant createdAt,
    Instant updatedAt,
    List<AdminLiveClassRegistrantResponse> registrants) {}
