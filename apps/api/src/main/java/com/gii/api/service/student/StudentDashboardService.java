package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentCertificateSummaryResponse;
import com.gii.api.model.response.student.StudentCourseSummaryResponse;
import com.gii.api.model.response.student.StudentDashboardResponse;
import com.gii.api.model.response.student.StudentLiveClassSummaryResponse;
import com.gii.api.model.response.student.StudentOngoingCourseSnapshot;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.user.User;
import com.gii.common.entity.user.UserProfile;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import com.gii.common.repository.user.UserProfileRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentDashboardService {

  private final CurrentUserService currentUserService;
  private final UserProfileRepository userProfileRepository;
  private final LessonProgressRepository lessonProgressRepository;
  private final EnrolledCoursesService enrolledCoursesService;
  private final StudentCertificatesService studentCertificatesService;
  private final StudentUpcomingLiveClasses studentUpcomingLiveClasses;

  public StudentDashboardResponse execute(Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);

    List<StudentCourseSummaryResponse> courses = enrolledCoursesService.execute(authentication);
    List<StudentCertificateSummaryResponse> certificates =
        studentCertificatesService.execute(authentication);
    List<StudentLiveClassSummaryResponse> upcoming =
        studentUpcomingLiveClasses.execute(authentication);

    List<StudentOngoingCourseSnapshot> ongoing =
        courses.stream()
            .filter(c -> c.completionPercentage() != null && c.completionPercentage() < 100.0)
            .sorted(
                Comparator.comparing(
                    StudentCourseSummaryResponse::enrolledAt,
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5)
            .map(this::toOngoingSnapshot)
            .toList();

    int completedCourses =
        (int)
            courses.stream()
                .filter(c -> c.completionPercentage() != null && c.completionPercentage() >= 100.0)
                .count();

    Instant lastLearningActivityAt =
        lessonProgressRepository.findLatestActivityAtByUserId(user.getId());

    return StudentDashboardResponse.builder()
        .fullName(user.getFullName())
        .email(user.getEmail())
        .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
        .totalEnrolledCourses(courses.size())
        .totalEarnedCertificates(
            (int) certificates.stream().filter(c -> !Boolean.TRUE.equals(c.isRevoked())).count())
        .completedCourses(completedCourses)
        .ongoingCourses(ongoing)
        .recentCertificates(certificates.stream().limit(5).toList())
        .upcomingLiveClasses(upcoming.stream().limit(5).toList())
        .learningStreak(null)
        .lastLearningActivityAt(
            lastLearningActivityAt != null ? lastLearningActivityAt.toString() : null)
        .build();
  }

  private StudentOngoingCourseSnapshot toOngoingSnapshot(StudentCourseSummaryResponse course) {
    return StudentOngoingCourseSnapshot.builder()
        .courseId(course.courseId())
        .courseName(course.courseName())
        .courseSlug(course.courseSlug())
        .courseThumbnailUrl(course.courseThumbnailUrl())
        .completionPercentage(course.completionPercentage())
        .completedLessons(course.completedLessons())
        .totalLessons(course.totalLessons())
        .enrolledAt(course.enrolledAt())
        .expiresAt(course.expiresAt())
        .isExpiring(
            course.expiresAt() != null
                && course.expiresAt().isBefore(Instant.now().plusSeconds(7 * 24 * 3600L)))
        .nextLessonId(null)
        .build();
  }
}
