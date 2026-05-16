package com.gii.api.model.response.admin;

import com.gii.common.enums.CollectionType;
import com.gii.common.enums.PublishStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminCollectionSummaryResponse(
    UUID collectionId,
    String title,
    String slug,
    CollectionType collectionType,
    PublishStatus status,
    BigDecimal priceBdt,
    Integer courseCount,
    Instant publishedAt,
    Instant createdAt) {}
