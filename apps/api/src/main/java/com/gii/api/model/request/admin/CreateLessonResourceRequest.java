package com.gii.api.model.request.admin;

import com.gii.common.enums.LessonResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateLessonResourceRequest(
    @NotNull UUID lessonId,
    @NotNull LessonResourceType resourceType,
    String title,
    String mimeType,
    @NotBlank String fileUrl,
    @NotNull Integer position) {}
