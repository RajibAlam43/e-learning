package com.gii.api.model.request.admin;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record UpdateCourseRequest(
    String title,
    String slug,
    String thumbnailUrl,
    String shortDescription,
    String description,
    List<String> highlights,
    BigDecimal priceBdt,
    List<String> courseOutcomes,
    List<String> requirements,
    String level, // CourseLevel enum as string
    String language, // CourseLanguage enum as string
    String studyMode, // StudyMode enum as string
    Boolean isFree,
    Integer estimatedDurationMinutes,
    String targetAudience,
    String prerequisites) {}
