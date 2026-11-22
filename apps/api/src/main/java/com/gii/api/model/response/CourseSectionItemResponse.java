package com.gii.api.model.response;

import com.gii.common.enums.SectionItemType;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CourseSectionItemResponse(
    UUID itemId,
    SectionItemType itemType,
    Integer position,
    LessonSummaryResponse lesson,
    CourseQuizSummaryResponse quiz,
    CourseLiveClassSummaryResponse liveClass) {}
