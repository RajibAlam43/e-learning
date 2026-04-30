package com.gii.api.model.response.admin;

import com.gii.common.enums.LessonResourceType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminLessonResourceResponse(
    UUID resourceId,
    UUID lessonId,
    String title,
    LessonResourceType resourceType, // PDF, IMAGE
    String mimeType, // e.g., "application/pdf", "image/png"
    String fileUrl,
    Integer position, // Display order within lesson
    Instant createdAt,
    Instant updatedAt) {}
