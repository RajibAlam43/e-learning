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
    String displayName,
    String headline,
    String headlineEn,
    String institution,
    String institutionEn,
    String expertiseArea,
    String expertiseAreaEn,
    String about,
    String aboutEn,
    String photoUrl,
    Boolean isPublic,
    String credentialsText,
    String credentialsTextEn,
    List<String> specialties,
    List<String> specialtiesEn,
    Integer yearsExperience,
    Instant createdAt,
    Instant updatedAt,
    List<AdminCourseSummaryResponse> assignedCourses) {}
