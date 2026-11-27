package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegisterProgramCourseRequest(@NotNull UUID courseId) {}
