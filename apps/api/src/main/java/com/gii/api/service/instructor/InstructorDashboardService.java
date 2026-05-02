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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
  private static final String DISPLAY_TIMEZONE = "Asia/Dhaka";
  private static final ZoneId DISPLAY_ZONE_ID = ZoneId.of(DISPLAY_TIMEZONE);
  private static final DateTimeFormatter TIME_LABEL_FORMATTER =
      DateTimeFormatter.ofPattern("EEE, MMM d, h:mm a z", Locale.US).withZone(DISPLAY_ZONE_ID);

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
    Map<UUID, Integer> activeEnrollmentsByCourseId =
        courseIds.isEmpty()
            ? Map.of()
            : toCountMap(
                enrollmentRepository.countByCourseIdsAndStatus(courseIds, EnrollmentStatus.ACTIVE));
    Map<UUID, Integer> completedEnrollmentsByCourseId =
        courseIds.isEmpty()
            ? Map.of()
            : toCountMap(
                enrollmentRepository.countCompletedByCourseIdsAndStatus(
                    courseIds, EnrollmentStatus.ACTIVE));
    Map<UUID, Integer> sectionCountByCourseId =
        courseIds.isEmpty()
            ? Map.of()
            : toCountMap(courseSectionRepository.countByCourseIds(courseIds));
    Map<UUID, Integer> lessonCountByCourseId =
        courseIds.isEmpty()
            ? Map.of()
            : toCountMap(
                lessonRepository.countByCourseIdsAndStatus(courseIds, PublishStatus.PUBLISHED));
    Map<UUID, Integer> liveClassCountByCourseId =
        courseIds.isEmpty()
            ? Map.of()
            : toCountMap(liveClassRepository.countByCourseIds(courseIds));

    Map<UUID, InstructorRole> roleByCourseId =
        assignments.stream()
            .collect(
                Collectors.toMap(
                    ci -> ci.getCourse().getId(), CourseInstructor::getRole, (a, b) -> a));

    List<InstructorCourseSnapshotResponse> snapshots =
        courses.stream()
            .map(
                course ->
                    toCourseSnapshot(
                        course,
                        roleByCourseId.get(course.getId()),
                        activeEnrollmentsByCourseId,
                        completedEnrollmentsByCourseId,
                        sectionCountByCourseId,
                        lessonCountByCourseId,
                        liveClassCountByCourseId))
            .toList();

    List<LiveClass> upcoming =
        courseIds.isEmpty()
            ? List.of()
            : liveClassRepository.findUpcomingByCourseIds(
                courseIds, List.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE), Instant.now());

    List<InstructorUpcomingLiveClassResponse> upcomingResponses =
        toUpcomingLiveClassResponses(upcoming, instructor.getId());

    int activeCourses =
        (int) courses.stream().filter(c -> c.getStatus() == PublishStatus.PUBLISHED).count();
    int totalStudents =
        courseIds.stream()
            .mapToInt(courseId -> activeEnrollmentsByCourseId.getOrDefault(courseId, 0))
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

  private List<InstructorUpcomingLiveClassResponse> toUpcomingLiveClassResponses(
      List<LiveClass> upcoming, UUID instructorId) {
    List<LiveClass> filtered =
        upcoming.stream()
            .filter(
                lc -> lc.getInstructor() != null && lc.getInstructor().getId().equals(instructorId))
            .limit(10)
            .toList();

    Map<UUID, Integer> approvedRegistrantCountByLiveClassId =
        filtered.isEmpty()
            ? Map.of()
            : toCountMap(
                liveClassRegistrantRepository.countByLiveClassIdsAndStatus(
                    filtered.stream().map(LiveClass::getId).toList(),
                    com.gii.common.enums.LiveClassRegistrantStatus.APPROVED));

    return filtered.stream()
        .map(lc -> toUpcomingLiveClass(lc, approvedRegistrantCountByLiveClassId))
        .toList();
  }

  private InstructorCourseSnapshotResponse toCourseSnapshot(
      Course course,
      InstructorRole role,
      Map<UUID, Integer> activeEnrollmentsByCourseId,
      Map<UUID, Integer> completedEnrollmentsByCourseId,
      Map<UUID, Integer> sectionCountByCourseId,
      Map<UUID, Integer> lessonCountByCourseId,
      Map<UUID, Integer> liveClassCountByCourseId) {
    int totalEnrolled = activeEnrollmentsByCourseId.getOrDefault(course.getId(), 0);
    int completed = completedEnrollmentsByCourseId.getOrDefault(course.getId(), 0);
    int totalSections = sectionCountByCourseId.getOrDefault(course.getId(), 0);
    int totalLessons = lessonCountByCourseId.getOrDefault(course.getId(), 0);
    int liveClassCount = liveClassCountByCourseId.getOrDefault(course.getId(), 0);

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

  private InstructorUpcomingLiveClassResponse toUpcomingLiveClass(
      LiveClass liveClass, Map<UUID, Integer> approvedRegistrantCountByLiveClassId) {
    return InstructorUpcomingLiveClassResponse.builder()
        .liveClassId(liveClass.getId())
        .title(liveClass.getTitle())
        .description(liveClass.getDescription())
        .startsAt(liveClass.getStartsAt())
        .endsAt(liveClass.getEndsAt())
        .timeLabel(TIME_LABEL_FORMATTER.format(liveClass.getStartsAt()))
        .courseId(liveClass.getCourse().getId())
        .courseName(liveClass.getCourse().getTitle())
        .sectionTitle(liveClass.getSection().getTitle())
        .lessonTitle(liveClass.getLesson().getTitle())
        .status(liveClass.getStatus())
        .registeredStudents(approvedRegistrantCountByLiveClassId.getOrDefault(liveClass.getId(), 0))
        .maxCapacity(null)
        .startUrl("/instructor/live-classes/" + liveClass.getId() + "/start")
        .detailsUrl("/instructor/live-classes/" + liveClass.getId())
        .build();
  }

  private Map<UUID, Integer> toCountMap(List<Object[]> rows) {
    Map<UUID, Integer> result = new HashMap<>();
    for (Object[] row : rows) {
      result.put((UUID) row[0], ((Long) row[1]).intValue());
    }
    return result;
  }
}
