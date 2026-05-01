package com.gii.api.model.response.instructor;

import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record InstructorCourseSnapshotResponse(
    UUID courseId,
    String courseName,
    String courseSlug,
    PublishStatus status, // DRAFT, PUBLISHED, ARCHIVED

    // Enrollment metrics
    Integer totalEnrolledStudents,
    Integer completedStudents,

    // Content metrics
    Integer totalSections,
    Integer totalLessons,
    Integer liveClassCount,
    Integer quizCount,

    // Instructor role
    InstructorRole role, // PRIMARY or ASSISTANT

    // Timestamps
    Instant createdAt,
    Instant publishedAt,

    // Quick actions
    String editUrl, // Link to edit course
    String analyticsUrl // Link to course analytics
) {}
