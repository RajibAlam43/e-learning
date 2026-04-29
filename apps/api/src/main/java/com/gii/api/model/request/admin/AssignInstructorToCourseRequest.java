package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AssignInstructorToCourseRequest(
        @NotNull UUID instructorUserId,
        @NotBlank String role  // PRIMARY, ASSISTANT
) {}