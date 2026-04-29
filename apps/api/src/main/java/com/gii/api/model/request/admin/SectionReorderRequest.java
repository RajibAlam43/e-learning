package com.gii.api.model.request.admin;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record SectionReorderRequest(
        UUID sectionId,
        Integer newPosition,
        List<LessonReorderRequest> lessons
) {}
