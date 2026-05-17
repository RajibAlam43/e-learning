package com.gii.api.model.request.admin;

import com.gii.common.enums.CollectionType;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record UpdateCollectionRequest(
    String title,
    String slug,
    CollectionType collectionType,
    String thumbnailObjectKey,
    String shortDescription,
    String description,
    @DecimalMin("0.0") BigDecimal priceBdt) {}
