package com.gii.api.model.response.admin;

import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminLiveClassSectionItemResponse(
    UUID liveClassId,
    String title,
    String titleEn,
    Instant startsAt,
    Instant endsAt,
    LiveClassProvider provider,
    LiveClassStatus status) {}
