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
    String slug,
    String thumbnailUrl,
    String shortDescription,
    String description,
    List<String> highlights,
    BigDecimal priceBdt,
    List<String> courseOutcomes,
    List<String> requirements,
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
    String prerequisites,
    UUID createdBy,
    Instant publishedAt,
    Instant createdAt,
    Instant updatedAt,
    List<AdminCourseSectionResponse> sections,
    List<AdminInstructorSummaryResponse> instructors) {}
