package com.gii.api.model.request.admin;

import com.gii.common.enums.CollectionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateCollectionRequest(
    @NotBlank String title,
    String titleEn,
    @NotBlank String slug,
    @NotNull CollectionType collectionType,
    String thumbnailObjectKey,
    String shortDescription,
    String shortDescriptionEn,
    String description,
    String descriptionEn,
    @NotNull @DecimalMin("0.0") BigDecimal priceBdt) {}
