package com.gii.api.model.request.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateThumbnailUploadRequest(
    @NotNull ThumbnailOwnerType ownerType,
    @NotBlank @Size(max = 200) String filename,
    @NotBlank String contentType,
    @Positive @Max(20 * 1024 * 1024) long sizeBytes) {}
