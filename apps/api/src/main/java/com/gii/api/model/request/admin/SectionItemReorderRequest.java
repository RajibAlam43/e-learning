package com.gii.api.model.request.admin;

import com.gii.common.enums.SectionItemType;
import java.util.UUID;
import lombok.Builder;

@Builder
public record SectionItemReorderRequest(
    UUID itemId, SectionItemType itemType, Integer newPosition) {}
