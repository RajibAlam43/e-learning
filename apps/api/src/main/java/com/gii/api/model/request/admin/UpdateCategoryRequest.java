package com.gii.api.model.request.admin;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateCategoryRequest(
    @Size(max = 150) String name,
    @Size(max = 150) String nameEn,
    @Size(max = 180) String slug,
    UUID parentId) {}
