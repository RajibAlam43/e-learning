package com.gii.api.model.request.admin;

import com.gii.common.enums.LessonResourceType;
import lombok.Builder;

@Builder
public record UpdateLessonResourceRequest(
    LessonResourceType resourceType,
    String title,
    String mimeType,
    String fileUrl,
    Integer position) {}
