package com.gii.api.model.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record InstructorDetailsResponse(
    UUID id,
    String slug,
    String fullName,
    String displayName,
    String avatarUrl,
    String headline,
    String institution,
    String expertiseArea,
    String about,
    String credentialsText,
    List<String> specialties,
    Integer yearsExperience,
    List<CourseSummaryResponse> publishedCourses) {}
