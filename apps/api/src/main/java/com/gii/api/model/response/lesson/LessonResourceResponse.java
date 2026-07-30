package com.gii.api.model.response.lesson;

import com.gii.common.enums.LessonResourceType;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LessonResourceResponse(
    UUID resourceId,
    String title,
    LessonResourceType resourceType, // PDF, IMAGE
    String mimeType, // e.g., "application/pdf", "image/png"
    Integer position, // Order position
    String downloadUrl // Direct download link (or temporary signed URL)
) {}
