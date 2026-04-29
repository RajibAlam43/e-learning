package com.gii.api.model.response.me;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

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
        List<String> specialties,  // From specialties_json
        Integer yearsExperience,
        Instant createdAt,
        Instant updatedAt
) {}