package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FeatureCourseRequest(@NotNull @Positive Integer position) {}
