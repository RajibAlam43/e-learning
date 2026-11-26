package com.gii.api.model.request.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SaveProgramCurriculumRequest(
    @NotNull @Min(1) @Max(100000) Integer requiredCredits,
    @NotNull @Min(0) @Max(100000) Integer electiveCreditLimit,
    @NotNull @Min(1) @Max(1200) Integer expectedDurationMonths,
    @Min(1) @Max(2400) Integer accessDurationMonths,
    @NotEmpty @Valid List<SetCollectionCoursesRequest.Item> items) {}
