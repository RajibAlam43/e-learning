package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateLessonVideoUploadRequest(
    @NotBlank @Size(max = 200) String filename,
    @NotBlank String contentType,
    @Positive long sizeBytes) {}
