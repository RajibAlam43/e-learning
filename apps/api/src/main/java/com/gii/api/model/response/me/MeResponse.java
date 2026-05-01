package com.gii.api.model.response.me;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder
public record MeResponse(
    UUID userId,
    String studentCode, // Unique student identifier

    // Basic user info
    String fullName,
    String email,
    String phone,
    String phoneCountryCode,

    // Account status
    String status, // ACTIVE, SUSPENDED, DELETED
    Boolean emailVerified,
    Boolean phoneVerified,
    Instant emailVerifiedAt,
    Instant phoneVerifiedAt,
    Instant createdAt,
    Instant updatedAt,

    // Roles and permissions
    Set<String> roles, // e.g., ["STUDENT", "INSTRUCTOR"]
    List<String> permissions, // Derived from roles

    // Profile info (from UserProfile)
    String avatarUrl,
    String locale, // e.g., "bn-BD"
    String timezone, // e.g., "Asia/Dhaka"
    String bio,
    java.util.Map<String, Object> extraJson, // Flexible profile data

    // Instructor profile (if applicable)
    InstructorProfileResponse instructorProfile,

    // Quick stats
    Integer totalEnrolledCourses,
    Integer completedCourses,
    Integer earnedCertificates,
    Integer totalLiveClassesAttended) {}
