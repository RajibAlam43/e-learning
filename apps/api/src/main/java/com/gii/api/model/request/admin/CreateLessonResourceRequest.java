package com.gii.api.model.request.admin;

import com.gii.common.enums.LessonResourcePurpose;
import com.gii.common.enums.LessonResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateLessonResourceRequest(
    @NotBlank @Size(max = 500) String title,
    @Size(max = 500) String titleEn,
    @NotNull LessonResourceType resourceType,
    LessonResourcePurpose purpose,
    @NotBlank String mimeType,
    @NotBlank String objectKey,
    @NotNull @Positive Integer position) {}
