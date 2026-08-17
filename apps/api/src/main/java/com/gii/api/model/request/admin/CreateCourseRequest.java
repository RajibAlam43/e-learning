package com.gii.api.model.request.admin;

import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.StudyMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateCourseRequest(
    @NotBlank String title,
    String titleEn,
    @NotBlank String slug,
    @NotEmpty List<@NotNull UUID> categoryIds,
    String thumbnailObjectKey,
    String shortDescription,
    String shortDescriptionEn,
    String description,
    String descriptionEn,
    List<String> highlights,
    List<String> highlightsEn,
    @NotNull BigDecimal priceBdt,
    List<String> courseOutcomes,
    List<String> courseOutcomesEn,
    List<String> requirements,
    List<String> requirementsEn,
    @NotNull CourseLevel level,
    @NotNull CourseLanguage language,
    @NotNull StudyMode studyMode,
    Boolean isFree,
    Integer estimatedDurationMinutes,
    String targetAudience,
    String targetAudienceEn,
    String prerequisites,
    String prerequisitesEn) {}
