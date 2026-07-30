package com.gii.api.model.response.admin;

import com.gii.common.enums.CollectionType;
import com.gii.common.enums.PublishStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminCollectionDetailResponse(
    UUID collectionId,
    String title,
    String titleEn,
    String slug,
    CollectionType collectionType,
    String thumbnailObjectKey,
    String thumbnailUrl,
    String shortDescription,
    String shortDescriptionEn,
    String description,
    String descriptionEn,
    BigDecimal priceBdt,
    PublishStatus status,
    Instant publishedAt,
    Instant createdAt,
    Instant updatedAt,
    List<AdminCollectionCourseResponse> courses) {}
