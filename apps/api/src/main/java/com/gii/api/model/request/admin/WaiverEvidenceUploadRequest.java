package com.gii.api.model.request.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WaiverEvidenceUploadRequest(
    @NotBlank @Size(max = 200) String fileName,
    @NotBlank String contentType,
    @Min(1) @Max(10485760) long sizeBytes) {}
