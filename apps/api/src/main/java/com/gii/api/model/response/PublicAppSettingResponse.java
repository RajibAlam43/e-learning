package com.gii.api.model.response;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;

@Builder
public record PublicAppSettingResponse(String key, Map<String, Object> value, Instant updatedAt) {}
