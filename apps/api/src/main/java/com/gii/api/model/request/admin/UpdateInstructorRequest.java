package com.gii.api.model.request.admin;

import java.util.List;
import lombok.Builder;

@Builder
public record UpdateInstructorRequest(
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
    Integer yearsExperience) {}
