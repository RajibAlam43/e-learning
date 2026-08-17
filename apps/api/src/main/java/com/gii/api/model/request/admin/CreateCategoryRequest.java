package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateCategoryRequest(
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Size(max = 150) String nameEn,
    @NotBlank @Size(max = 180) String slug,
    UUID parentId) {}
