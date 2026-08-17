package com.gii.api.model.response.admin;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminAppSettingResponse(
    UUID settingId,
    String key,
    Map<String, Object> value,
    String description,
    Boolean isPublic,
    Instant createdAt,
    Instant updatedAt) {}
