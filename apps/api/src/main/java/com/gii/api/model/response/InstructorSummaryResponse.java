package com.gii.api.model.response;

import java.util.UUID;
import lombok.Builder;

@Builder
public record InstructorSummaryResponse(
    UUID id,
    String slug,
    String fullName,
    String avatarUrl,
    String shortBio,
    String credentials,
    Integer yearsExperience,
    Integer publishedCoursesCount) {}
