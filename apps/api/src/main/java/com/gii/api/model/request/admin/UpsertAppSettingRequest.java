package com.gii.api.model.request.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpsertAppSettingRequest(
    @NotNull Map<String, Object> value,
    @Size(max = 1000) String description,
    @NotNull Boolean isPublic) {}
