package com.gii.api.model.response.me;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record InstructorProfileResponse(
    String displayName,
    String headline,
    String institution,
    String expertiseArea,
    String about,
    String photoUrl,
    Boolean isPublic,
    String credentialsText,
    List<String> specialties, // From specialties_json
    Integer yearsExperience,
    Instant createdAt,
    Instant updatedAt) {}
