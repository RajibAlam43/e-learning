package com.gii.api.model.request.admin;

import java.time.Instant;
import lombok.Builder;

@Builder
public record UpdateSectionRequest(
    String title,
    String titleEn,
    String slug,
    Integer position,
    String description,
    String descriptionEn,
    Boolean isMandatory,
    Boolean isFree,
    String releaseType,
    Instant releaseAt,
    Integer unlockAfterDays) {}
