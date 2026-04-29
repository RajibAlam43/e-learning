package com.gii.api.model.response.lesson;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ResourceDownloadUrlResponse(
        String downloadUrl,  // Signed temporary URL for file download
        Instant expiresAt,  // When the signed URL expires
        String fileName  // Original file name for Content-Disposition
) {}