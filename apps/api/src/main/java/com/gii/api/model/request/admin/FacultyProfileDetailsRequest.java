package com.gii.api.model.request.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FacultyProfileDetailsRequest(
    @Size(max = 2000) String credentialsText,
    @Size(max = 2000) String credentialsTextEn,
    @Size(max = 255) String institution,
    @Size(max = 255) String institutionEn,
    @Size(max = 255) String expertiseArea,
    @Size(max = 255) String expertiseAreaEn,
    @Size(max = 20) List<@NotBlank @Size(max = 100) String> specialties,
    @Size(max = 20) List<@NotBlank @Size(max = 100) String> specialtiesEn,
    @Min(0) @Max(100) Integer yearsExperience) {}
