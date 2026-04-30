package com.gii.api.model.response.instructor;

import java.util.List;
import lombok.Builder;

@Builder
public record InstructorDashboardResponse(
    // Instructor profile summary
    String instructorName,
    String displayName,
    String headline,
    String photoUrl,

    // Quick stats
    Integer totalCoursesAssigned,
    Integer activeCourses, // PUBLISHED courses
    Integer totalStudentsAcrossAllCourses,

    // Courses teaching
    List<InstructorCourseSnapshotResponse> assignedCourses,

    // Upcoming live classes (next 5-10)
    List<InstructorUpcomingLiveClassResponse> upcomingLiveClasses,

    // Performance metrics (optional)
    Double averageCourseRating,
    Integer totalReviews) {}
