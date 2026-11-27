package com.gii.api.model.response.admin;

import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminCollectionCourseResponse(
    UUID courseId,
    String courseTitle,
    String courseSlug,
    Integer position,
    Boolean isMandatory,
    UUID courseTemplateId,
    Integer credits,
    com.gii.common.enums.PublishStatus status) {}
