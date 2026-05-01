package com.gii.api.model.response;

import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.StudyMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CourseDetailsResponse(
    UUID id,
    String title,
    String slug,
    String thumbnailUrl,
    String shortDescription,
    String description,
    List<String> highlights,
    BigDecimal priceBdt,
    List<String> courseOutcomes,
    List<String> requirements,
    List<String> prerequisites,
    CourseLevel level,
    CourseLanguage language,
    StudyMode studyMode,
    List<CategoryResponse> categories,
    List<CourseSectionResponse> sections,
    List<InstructorSummaryResponse> instructors,
    Instant publishedAt,
    Integer liveSessionCount,
    Integer quizCount,
    Integer recordedHoursCount,
    Boolean isFree) {}
