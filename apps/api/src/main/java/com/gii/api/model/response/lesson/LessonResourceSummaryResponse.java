package com.gii.api.model.response.lesson;

import com.gii.common.enums.LessonResourcePurpose;
import com.gii.common.enums.LessonResourceType;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LessonResourceSummaryResponse(
    UUID resourceId,
    String title,
    LessonResourceType resourceType,
    LessonResourcePurpose purpose,
    String mimeType,
    Integer position) {}
