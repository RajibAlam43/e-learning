package com.gii.api.model.request.admin;

import com.gii.common.enums.MediaProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record CourseVideoRequest(
    @NotNull MediaProvider provider,
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{11}$", message = "must be a valid YouTube video ID")
        String sourceId) {}
