package com.gii.api.service.instructor;

import com.gii.api.model.response.instructor.InstructorCourseSnapshotResponse;
import com.gii.api.model.response.instructor.InstructorDashboardResponse;
import com.gii.api.model.response.instructor.InstructorUpcomingLiveClassResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.live.LiveClassRegistrantRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorDashboardService {

  private final CurrentUserService currentUserService;
  private final InstructorProfileRepository instructorProfileRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CourseSectionRepository courseSectionRepository;
  private final LessonRepository lessonRepository;
  private final LiveClassRepository liveClassRepository;
  private final LiveClassRegistrantRepository liveClassRegistrantRepository;

  public InstructorDashboardResponse execute(Authentication authentication) {
    User instructor = currentUserService.getCurrentUser(authentication);
    InstructorProfile profile =
        instructorProfileRepository.findById(instructor.getId()).orElse(null);

    List<CourseInstructor> assignments =
        courseInstructorRepository.findByInstructorId(instructor.getId());
    List<Course> courses = assignments.stream().map(CourseInstructor::getCourse).toList();
    List<UUID> courseIds = courses.stream().map(Course::getId).toList();

    Map<UUID, InstructorRole> roleByCourseId =
        assignments.stream()
            .collect(
                Collectors.toMap(
                    ci -> ci.getCourse().getId(), CourseInstructor::getRole, (a, b) -> a));

    List<InstructorCourseSnapshotResponse> snapshots =
        courses.stream()
            .map(course -> toCourseSnapshot(course, roleByCourseId.get(course.getId())))
            .toList();

    List<LiveClass> upcoming =
        courseIds.isEmpty()
            ? List.of()
            : liveClassRepository.findUpcomingByCourseIds(
                courseIds, List.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE), Instant.now());

    List<InstructorUpcomingLiveClassResponse> upcomingResponses =
        upcoming.stream()
            .filter(
                lc ->
                    lc.getInstructor() != null
                        && lc.getInstructor().getId().equals(instructor.getId()))
            .limit(10)
            .map(this::toUpcomingLiveClass)
            .toList();

    int activeCourses =
        (int) courses.stream().filter(c -> c.getStatus() == PublishStatus.PUBLISHED).count();
    int totalStudents =
        courseIds.stream()
            .mapToInt(
                courseId ->
                    (int)
                        enrollmentRepository.countByCourseIdAndStatus(
                            courseId, EnrollmentStatus.ACTIVE))
            .sum();

    return InstructorDashboardResponse.builder()
        .instructorName(instructor.getFullName())
        .displayName(profile != null ? profile.getDisplayName() : instructor.getFullName())
        .headline(profile != null ? profile.getHeadline() : null)
        .photoUrl(profile != null ? profile.getPhotoUrl() : null)
        .totalCoursesAssigned(courses.size())
        .activeCourses(activeCourses)
        .totalStudentsAcrossAllCourses(totalStudents)
        .assignedCourses(snapshots)
        .upcomingLiveClasses(upcomingResponses)
        .averageCourseRating(null)
        .totalReviews(null)
        .build();
  }

  private InstructorCourseSnapshotResponse toCourseSnapshot(Course course, InstructorRole role) {
    int totalEnrolled =
        (int)
            enrollmentRepository.countByCourseIdAndStatus(course.getId(), EnrollmentStatus.ACTIVE);
    int completed =
        (int)
            enrollmentRepository.countByCourseIdAndStatusAndCompletedAtIsNotNull(
                course.getId(), EnrollmentStatus.ACTIVE);
    int totalSections =
        courseSectionRepository.findByCourseIdOrderByPositionAsc(course.getId()).size();
    int totalLessons = lessonRepository.findByCourseIdOrderByPositionAsc(course.getId()).size();
    int liveClassCount =
        liveClassRepository.findByCourseIdOrderByStartsAtAsc(course.getId()).size();

    return InstructorCourseSnapshotResponse.builder()
        .courseId(course.getId())
        .courseName(course.getTitle())
        .courseSlug(course.getSlug())
        .status(course.getStatus())
        .totalEnrolledStudents(totalEnrolled)
        .completedStudents(completed)
        .totalSections(totalSections)
        .totalLessons(totalLessons)
        .liveClassCount(liveClassCount)
        .quizCount(course.getQuizCount())
        .role(role)
        .createdAt(course.getCreatedAt())
        .publishedAt(course.getPublishedAt())
        .editUrl("/admin/courses/" + course.getId())
        .analyticsUrl("/admin/courses/" + course.getId() + "/analytics")
        .build();
  }

  private InstructorUpcomingLiveClassResponse toUpcomingLiveClass(LiveClass liveClass) {
    return InstructorUpcomingLiveClassResponse.builder()
        .liveClassId(liveClass.getId())
        .title(liveClass.getTitle())
        .description(liveClass.getDescription())
        .startsAt(liveClass.getStartsAt())
        .endsAt(liveClass.getEndsAt())
        .timeLabel(null)
        .courseId(liveClass.getCourse().getId())
        .courseName(liveClass.getCourse().getTitle())
        .sectionTitle(liveClass.getSection().getTitle())
        .lessonTitle(liveClass.getLesson().getTitle())
        .status(liveClass.getStatus())
        .registeredStudents(
            (int)
                liveClassRegistrantRepository.countByLiveClassIdAndStatus(
                    liveClass.getId(), com.gii.common.enums.LiveClassRegistrantStatus.APPROVED))
        .maxCapacity(null)
        .startUrl("/instructor/live-classes/" + liveClass.getId() + "/start")
        .detailsUrl("/instructor/live-classes/" + liveClass.getId())
        .build();
  }
}
