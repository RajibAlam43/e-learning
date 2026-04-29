package com.gii.api.model.response.student;

import lombok.Builder;

import java.util.List;

@Builder
public record StudentDashboardResponse(
        // Student info
        String fullName,
        String email,
        String avatarUrl,

        // Quick stats
        Integer totalEnrolledCourses,
        Integer totalEarnedCertificates,
        Integer completedCourses,

        // Ongoing courses with quick progress snapshots
        List<StudentOngoingCourseSnapshot> ongoingCourses,

        // Recent certificates
        List<StudentCertificateSummaryResponse> recentCertificates,

        // Upcoming live classes (next 3-5)
        List<StudentLiveClassSummaryResponse> upcomingLiveClasses,

        // Learning streak info (optional)
        Integer learningStreak,
        String lastLearningActivityAt
) {}