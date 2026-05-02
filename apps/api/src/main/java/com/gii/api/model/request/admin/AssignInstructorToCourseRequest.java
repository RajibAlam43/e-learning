package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AssignInstructorToCourseRequest(
    @NotNull UUID instructorUserId, @NotBlank String role // PRIMARY, ASSISTANT
    ) {}
