package com.gii.api.model.request.student;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCourseReviewRequest(
    @NotNull @Min(1) @Max(5) Integer rating,
    @NotBlank @Size(max = 5000) String reviewText) {}
