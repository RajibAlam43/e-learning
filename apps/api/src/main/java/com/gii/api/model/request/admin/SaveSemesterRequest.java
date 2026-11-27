package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record SaveSemesterRequest(
    @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
    @NotBlank @Size(max = 150) String name,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @NotNull Instant registrationStartsAt,
    @NotNull Instant registrationEndsAt) {}
