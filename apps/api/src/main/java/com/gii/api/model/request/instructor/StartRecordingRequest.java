package com.gii.api.model.request.instructor;

import com.gii.common.enums.RecordingMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record StartRecordingRequest(
    @NotNull RecordingMethod method,
    @NotNull @Pattern(regexp = "video/mp4;codecs=avc1(?:\\.[a-zA-Z0-9]+)?,mp4a\\.40\\.2")
        String mimeType) {}
