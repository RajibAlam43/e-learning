package com.gii.api.model.request.admin;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record UpdateCourseRequest(
    String title,
    String titleEn,
    String slug,
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
