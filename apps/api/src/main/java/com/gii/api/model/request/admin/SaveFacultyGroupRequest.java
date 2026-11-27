package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SaveFacultyGroupRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 200) String nameEn,
    @NotNull @Positive Integer position,
    @NotNull Boolean visible) {}
