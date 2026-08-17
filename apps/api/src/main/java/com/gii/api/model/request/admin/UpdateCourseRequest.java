package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateCourseRequest(
    String title,
    String titleEn,
    String slug,
    @Size(min = 1) List<@NotNull UUID> categoryIds,
    String thumbnailObjectKey,
    String shortDescription,
    String shortDescriptionEn,
    String description,
    String descriptionEn,
    List<String> highlights,
    List<String> highlightsEn,
    BigDecimal priceBdt,
    List<String> courseOutcomes,
    List<String> courseOutcomesEn,
    List<String> requirements,
    List<String> requirementsEn,
    String level, // CourseLevel enum as string
    String language, // CourseLanguage enum as string
    String studyMode, // StudyMode enum as string
    Boolean isFree,
    Integer estimatedDurationMinutes,
    String targetAudience,
    String targetAudienceEn,
    String prerequisites,
    String prerequisitesEn) {}
