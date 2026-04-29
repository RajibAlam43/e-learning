package com.gii.api.model.request.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ReorderCourseStructureRequest(
        @NotEmpty @Valid List<SectionReorderRequest> sections
) {}

