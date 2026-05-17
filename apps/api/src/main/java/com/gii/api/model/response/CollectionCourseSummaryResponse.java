package com.gii.api.model.response;

import java.util.UUID;
import lombok.Builder;

@Builder
public record CollectionCourseSummaryResponse(
    UUID id, String title, String slug, String thumbnailUrl, Integer position, Boolean isMandatory) {}
