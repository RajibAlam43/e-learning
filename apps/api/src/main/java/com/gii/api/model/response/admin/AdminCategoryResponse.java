package com.gii.api.model.response.admin;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminCategoryResponse(
    UUID id, String name, String nameEn, String slug, UUID parentId, Instant createdAt) {}
