package com.gii.api.model.request.admin;

import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.StudyMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record CreateCourseRequest(
    @NotBlank String title,
    @NotBlank String slug,
    String thumbnailUrl,
    String shortDescription,
    String description,
    List<String> highlights,
    @NotNull BigDecimal priceBdt,
    List<String> courseOutcomes,
    List<String> requirements,
    @NotNull CourseLevel level,
    @NotNull CourseLanguage language,
    @NotNull StudyMode studyMode,
    Boolean isFree,
    Integer estimatedDurationMinutes,
    String targetAudience,
    String prerequisites) {}
