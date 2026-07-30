package com.gii.api.model.response.admin;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminLiveClassSummaryResponse(
    UUID liveClassId,
    String title,
    String titleEn,
    String courseName,
    String courseNameEn,
    String instructorName,
    String status, // SCHEDULED, LIVE, COMPLETED, CANCELLED, FAILED
    Instant startsAt,
    Integer approvedRegistrants,
    Instant createdAt) {}
