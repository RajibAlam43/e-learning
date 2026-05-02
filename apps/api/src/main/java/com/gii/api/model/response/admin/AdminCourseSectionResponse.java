package com.gii.api.model.response.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminCourseSectionResponse(
    UUID sectionId,
    String title,
    String slug,
    Integer position,
    String description,
    Boolean isMandatory,
    Boolean isFree,
    String status, // DRAFT, PUBLISHED, ARCHIVED
    String releaseType,
    Instant releaseAt,
    Integer unlockAfterDays,
    Instant publishedAt,
    Instant createdAt,
    Instant updatedAt,
    List<AdminSectionItemResponse> items) {}
