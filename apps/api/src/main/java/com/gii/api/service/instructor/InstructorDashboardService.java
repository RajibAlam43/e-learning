package com.gii.api.service.instructor;

import com.gii.api.model.response.instructor.InstructorCourseSnapshotResponse;
import com.gii.api.model.response.instructor.InstructorDashboardResponse;
import com.gii.api.model.response.instructor.InstructorUpcomingLiveClassResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReviewStatus;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseReviewRepository;
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
  private final CourseReviewRepository courseReviewRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CourseSectionRepository courseSectionRepository;
  private final LessonRepository lessonRepository;
  private final LiveClassRepository liveClassRepository;
  private final LiveClassRegistrantRepository liveClassRegistrantRepository;
  private final LocalizedContentService localizedContentService;

  public InstructorDashboardResponse execute(Authentication authentication) {
    User instructor = currentUserService.getCurrentUser(authentication);
    InstructorProfile profile =
        instructorProfileRepository.findById(instructor.getId()).orElse(null);

    List<CourseInstructor> assignments =
        courseInstructorRepository.findByInstructorId(instructor.getId());
    List<Course> courses = assignments.stream().map(CourseInstructor::getCourse).toList();
    List<UUID> courseIds = courses.stream().map(Course::getId).toList();
    Map<UUID, Long> activeEnrollmentsByCourseId =
        courseIds.isEmpty()
            ? Map.of()
            : toCountMap(
                enrollmentRepository.countByCourseIdsAndStatus(courseIds, EnrollmentStatus.ACTIVE));
    Map<UUID, Long> completedEnrollmentsByCourseId =
        courseIds.isEmpty()
            ? Map.of()
            : toCountMap(
                enrollmentRepository.countCompletedByCourseIdsAndStatus(
                    courseIds, EnrollmentStatus.ACTIVE));
    Map<UUID, Long> sectionCountByCourseId =
        courseIds.isEmpty()
            ? Map.of()
            : toCountMap(courseSectionRepository.countByCourseIds(courseIds));
    Map<UUID, Long> lessonCountByCourseId =
        courseIds.isEmpty()
            ? Map.of()
            : toCountMap(
                lessonRepository.countByCourseIdsAndStatus(courseIds, PublishStatus.PUBLISHED));
    Map<UUID, Long> liveClassCountByCourseId =
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
    long totalStudents =
        courseIds.stream()
            .mapToLong(courseId -> activeEnrollmentsByCourseId.getOrDefault(courseId, 0L))
            .sum();
    ReviewAggregate reviewAggregate = aggregateReviews(courseIds);

    return InstructorDashboardResponse.builder()
        .instructorName(instructor.getFullName())
        .displayName(profile != null ? profile.getDisplayName() : instructor.getFullName())
        .headline(
            profile != null
                ? localizedContentService.text(profile.getHeadline(), profile.getHeadlineEn())
                : null)
        .photoUrl(profile != null ? profile.getPhotoUrl() : null)
        .totalCoursesAssigned(courses.size())
        .activeCourses(activeCourses)
        .totalStudentsAcrossAllCourses(totalStudents)
        .assignedCourses(snapshots)
        .upcomingLiveClasses(upcomingResponses)
        .averageCourseRating(reviewAggregate.averageRating())
        .totalReviews(reviewAggregate.totalReviews())
        .build();
  }

  private ReviewAggregate aggregateReviews(List<UUID> courseIds) {
    if (courseIds.isEmpty()) {
      return new ReviewAggregate(null, 0L);
    }
    return aggregateReviewRows(
        courseReviewRepository.aggregateByCourseIdsAndStatus(courseIds, ReviewStatus.PUBLISHED));
  }

  static ReviewAggregate aggregateReviewRows(List<Object[]> rows) {
    double weightedRating = 0;
    long totalReviews = 0;
    for (Object[] row : rows) {
      long count = ((Number) row[2]).longValue();
      weightedRating += ((Number) row[1]).doubleValue() * count;
      totalReviews = Math.addExact(totalReviews, count);
    }
    return new ReviewAggregate(
        totalReviews == 0 ? null : weightedRating / totalReviews, totalReviews);
  }

  record ReviewAggregate(Double averageRating, Long totalReviews) {}

  private List<InstructorUpcomingLiveClassResponse> toUpcomingLiveClassResponses(
      List<LiveClass> upcoming, UUID instructorId) {
    List<LiveClass> filtered =
        upcoming.stream()
            .filter(
                lc -> lc.getInstructor() != null && lc.getInstructor().getId().equals(instructorId))
            .limit(10)
            .toList();

    Map<UUID, Long> approvedRegistrantCountByLiveClassId =
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
      Map<UUID, Long> activeEnrollmentsByCourseId,
      Map<UUID, Long> completedEnrollmentsByCourseId,
      Map<UUID, Long> sectionCountByCourseId,
      Map<UUID, Long> lessonCountByCourseId,
      Map<UUID, Long> liveClassCountByCourseId) {
    long totalEnrolled = activeEnrollmentsByCourseId.getOrDefault(course.getId(), 0L);
    long completed = completedEnrollmentsByCourseId.getOrDefault(course.getId(), 0L);
    long totalSections = sectionCountByCourseId.getOrDefault(course.getId(), 0L);
    long totalLessons = lessonCountByCourseId.getOrDefault(course.getId(), 0L);
    long liveClassCount = liveClassCountByCourseId.getOrDefault(course.getId(), 0L);

    return InstructorCourseSnapshotResponse.builder()
        .courseId(course.getId())
        .courseName(localizedContentService.text(course.getTitle(), course.getTitleEn()))
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
      LiveClass liveClass, Map<UUID, Long> approvedRegistrantCountByLiveClassId) {
    return InstructorUpcomingLiveClassResponse.builder()
        .liveClassId(liveClass.getId())
        .title(localizedContentService.text(liveClass.getTitle(), liveClass.getTitleEn()))
        .description(
            localizedContentService.text(liveClass.getDescription(), liveClass.getDescriptionEn()))
        .startsAt(liveClass.getStartsAt())
        .endsAt(liveClass.getEndsAt())
        .timeLabel(TIME_LABEL_FORMATTER.format(liveClass.getStartsAt()))
        .courseId(liveClass.getCourse().getId())
        .courseName(
            localizedContentService.text(
                liveClass.getCourse().getTitle(), liveClass.getCourse().getTitleEn()))
        .sectionTitle(
            localizedContentService.text(
                liveClass.getSection().getTitle(), liveClass.getSection().getTitleEn()))
        .status(liveClass.getStatus())
        .registeredStudents(
            approvedRegistrantCountByLiveClassId.getOrDefault(liveClass.getId(), 0L))
        .maxCapacity(null)
        .startUrl("/instructor/live-classes/" + liveClass.getId() + "/start")
        .detailsUrl("/instructor/live-classes/" + liveClass.getId())
        .build();
  }

  static Map<UUID, Long> toCountMap(List<Object[]> rows) {
    Map<UUID, Long> result = new HashMap<>();
    for (Object[] row : rows) {
      result.put((UUID) row[0], ((Number) row[1]).longValue());
    }
    return result;
  }
}
