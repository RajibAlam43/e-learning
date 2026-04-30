package com.gii.api.model.request.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Builder;

@Builder
public record ReorderCourseStructureRequest(
    @NotEmpty @Valid List<SectionReorderRequest> sections) {}
