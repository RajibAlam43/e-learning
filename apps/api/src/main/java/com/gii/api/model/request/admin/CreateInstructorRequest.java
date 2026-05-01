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
    String phoneCountryCode,
    @NotBlank String displayName,
    String headline,
    String institution,
    String expertiseArea,
    String about,
    String photoUrl,
    Boolean isPublic,
    String credentialsText,
    List<String> specialties,
    Integer yearsExperience) {}
