package com.gii.api.model.response.admin;

import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminCourseDetailResponse(
    UUID courseId,
    String title,
    String titleEn,
    String slug,
    String thumbnailObjectKey,
    String thumbnailUrl,
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
    CourseLevel level,
    CourseLanguage language,
    StudyMode studyMode,
    PublishStatus status,
    Boolean isFree,
    Integer liveSessionCount,
    Integer quizCount,
    Integer recordedHoursCount,
    Integer estimatedDurationMinutes,
    String targetAudience,
    String targetAudienceEn,
    String prerequisites,
    String prerequisitesEn,
    UUID createdBy,
    Instant publishedAt,
    Instant createdAt,
    Instant updatedAt,
    List<AdminCourseSectionResponse> sections,
    List<AdminInstructorSummaryResponse> instructors) {}
