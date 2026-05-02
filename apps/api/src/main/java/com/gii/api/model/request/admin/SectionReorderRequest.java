package com.gii.api.model.request.admin;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record SectionReorderRequest(
    UUID sectionId, Integer newPosition, List<SectionItemReorderRequest> items) {}
