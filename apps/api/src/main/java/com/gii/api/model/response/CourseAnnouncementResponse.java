package com.gii.api.model.response;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CourseAnnouncementResponse(
    UUID announcementId,
    UUID courseId,
    String courseTitle,
    String title,
    String content,
    Instant createdAt) {}
