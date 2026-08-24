package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateLiveClassItemRequest(
    @NotNull UUID sectionId,
    @Positive Integer position,
    @NotBlank String title,
    String titleEn,
    String description,
    String descriptionEn,
    @Positive Integer expectedDurationMinutes,
    Boolean isMandatory) {}
