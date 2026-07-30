package com.gii.api.model.request.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Builder;

@Builder
public record CreateInstructorRequest(
    @NotBlank String fullName,
    @Email String email,
    String phone,
    @NotBlank String displayName,
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
