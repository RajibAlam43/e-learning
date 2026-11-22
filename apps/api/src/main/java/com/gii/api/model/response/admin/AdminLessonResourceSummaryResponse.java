package com.gii.api.model.response.admin;

import com.gii.common.enums.LessonResourcePurpose;
import com.gii.common.enums.LessonResourceType;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminLessonResourceSummaryResponse(
    UUID resourceId,
    String title,
    String titleEn,
    LessonResourceType resourceType,
    LessonResourcePurpose purpose,
    String mimeType,
    Integer position) {}
