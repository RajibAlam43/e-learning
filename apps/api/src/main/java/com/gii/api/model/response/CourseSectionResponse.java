package com.gii.api.model.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CourseSectionResponse(
    UUID id,
    String title,
    String description,
    Integer position,
    List<LessonSummaryResponse> lessons) {}
