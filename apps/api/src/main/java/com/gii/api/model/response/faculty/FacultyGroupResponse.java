package com.gii.api.model.response.faculty;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FacultyGroupResponse(
    UUID id,
    String name,
    String nameEn,
    Integer position,
    Boolean visible,
    List<FacultyMemberResponse> members) {}
