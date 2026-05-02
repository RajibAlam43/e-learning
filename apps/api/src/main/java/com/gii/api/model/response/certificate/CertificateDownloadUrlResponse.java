package com.gii.api.model.response.certificate;

import java.time.Instant;
import lombok.Builder;

@Builder
public record CertificateDownloadUrlResponse(
    String downloadUrl, // Signed temporary URL for PDF download
    Instant expiresAt, // When the signed URL expires
    String fileName, // Suggested file name, e.g., "Certificate-CourseName.pdf"
    String contentType, // "application/pdf"
    Long fileSizeBytes, // Optional: file size for progress indicators
    String checksum // Optional: MD5/SHA256 for integrity verification
) {}
