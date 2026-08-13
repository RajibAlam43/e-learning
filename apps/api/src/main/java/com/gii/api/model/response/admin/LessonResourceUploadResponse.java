package com.gii.api.model.response.admin;

import java.time.Instant;
import lombok.Builder;

@Builder
public record LessonResourceUploadResponse(
    String objectKey,
    String uploadUrl,
    String method,
    String contentType,
    long sizeBytes,
    Instant expiresAt) {}
