package com.gii.api.model.response.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminInstructorDetailResponse(
    UUID userId,
    String fullName,
    String email,
    String phone,
    String phoneCountryCode,
    String displayName,
    String headline,
    String institution,
    String expertiseArea,
    String about,
    String photoUrl,
    Boolean isPublic,
    String credentialsText,
    List<String> specialties,
    Integer yearsExperience,
    Instant createdAt,
    Instant updatedAt,
    List<AdminCourseSummaryResponse> assignedCourses) {}
