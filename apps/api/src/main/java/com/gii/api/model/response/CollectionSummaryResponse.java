package com.gii.api.model.response;

import com.gii.common.enums.CollectionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CollectionSummaryResponse(
    UUID id,
    String title,
    String slug,
    CollectionType collectionType,
    String shortDescription,
    String thumbnailObjectKey,
    BigDecimal priceBdt,
    Instant publishedAt,
    Integer courseCount,
    List<String> instructorNames) {}
