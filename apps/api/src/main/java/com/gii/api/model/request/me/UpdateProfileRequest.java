package com.gii.api.model.request.me;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record UpdateProfileRequest(
    String fullName, // Update display name
    @Email String email, // Update email (may require re-verification)
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
        String phone, // Update phone (may require re-verification)
    String phoneCountryCode, // e.g., "+880"
    String avatarUrl, // Update profile picture URL
    String bio, // Update bio text
    String locale, // e.g., "bn-BD", "en-US"
    String timezone, // e.g., "Asia/Dhaka"

    // Instructor-specific (if applicable)
    String displayName, // Instructor display name
    String headline, // Instructor headline
    String institution, // Institution affiliation
    String expertiseArea, // Expertise area
    String about, // Detailed about text
    String photoUrl, // Instructor photo URL
    Boolean isPublic, // Whether instructor profile is public
    String credentialsText, // Credentials/certifications
    java.util.List<String> specialties, // List of specialties
    Integer yearsExperience // Years of experience
) {}
