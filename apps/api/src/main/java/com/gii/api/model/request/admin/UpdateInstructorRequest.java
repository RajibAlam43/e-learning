package com.gii.api.model.request.admin;

import java.util.List;
import lombok.Builder;

@Builder
public record UpdateInstructorRequest(
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
    Integer yearsExperience) {}
