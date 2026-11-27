package com.gii.api.model.request.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SaveFacultyMemberRequest(
    @NotNull UUID groupId,
    UUID instructorId,
    @NotBlank @Size(max = 200) String name,
    @Size(max = 300) String title,
    @Size(max = 300) String titleEn,
    @Size(max = 5000) String bio,
    @Size(max = 5000) String bioEn,
    @Size(max = 500) String photoObjectKey,
    @NotNull @Positive Integer position,
    @NotNull @Positive Integer homepagePosition,
    @NotNull Boolean showOnFaculty,
    @NotNull Boolean showOnHomepage,
    @Valid FacultyProfileDetailsRequest profileDetails) {}
