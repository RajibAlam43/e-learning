package com.gii.api.model.response.faculty;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FacultyMemberResponse(
    UUID id,
    UUID groupId,
    UUID instructorId,
    String name,
    String title,
    String titleEn,
    String bio,
    String bioEn,
    String photoUrl,
    String photoObjectKey,
    Integer position,
    Integer homepagePosition,
    Boolean showOnFaculty,
    Boolean showOnHomepage,
    Boolean profilePublic,
    String credentialsText,
    String institution,
    String expertiseArea,
    List<String> specialties,
    Integer yearsExperience,
    String credentialsTextEn,
    String institutionEn,
    String expertiseAreaEn,
    List<String> specialtiesEn) {}
