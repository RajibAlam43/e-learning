package com.gii.api.model.response.admin;

import java.time.Instant;
import lombok.Builder;

@Builder
public record LessonVideoUploadResponse(
    String uploadId,
    String uploadUrl,
    String method,
    String contentType,
    long sizeBytes,
    Instant expiresAt) {}
