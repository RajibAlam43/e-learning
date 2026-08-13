package com.gii.api.model.request.admin;

import com.gii.common.enums.LessonResourceType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateLessonResourceRequest(
    @Size(max = 500) String title,
    @Size(max = 500) String titleEn,
    LessonResourceType resourceType,
    String mimeType,
    String objectKey,
    @Positive Integer position) {}
