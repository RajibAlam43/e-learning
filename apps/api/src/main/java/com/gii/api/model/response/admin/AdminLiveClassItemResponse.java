package com.gii.api.model.response.admin;

import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminLiveClassItemResponse(
    UUID liveClassId,
    UUID sectionId,
    Integer position,
    String title,
    String titleEn,
    String description,
    String descriptionEn,
    Integer expectedDurationMinutes,
    Boolean isMandatory,
    Boolean scheduled) {}
