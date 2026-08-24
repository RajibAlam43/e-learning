package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentCourseHomeResponse;
import com.gii.api.model.response.student.StudentLessonHomeResponse;
import com.gii.api.model.response.student.StudentLiveClassHomeResponse;
import com.gii.api.model.response.student.StudentQuizHomeResponse;
import com.gii.api.model.response.student.StudentSectionHomeResponse;
import com.gii.api.model.response.student.StudentSectionItemResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.enrollment.CurriculumAccessService;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.progress.CourseCompletionService;
import com.gii.api.service.progress.CourseCompletionService.CourseCompletion;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.common.entity.certificate.Certificate;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.SectionItem;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassSlot;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.SectionItemType;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.SectionItemRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.live.LiveClassSlotRepository;
import com.gii.common.repository.quiz.QuizRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrolledCourseDetailsService {

  private static final Set<LiveClassStatus> COMPLETABLE_LIVE_CLASS_STATUSES =
      Set.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE, LiveClassStatus.COMPLETED);

  private final CurrentUserService currentUserService;
  private final EnrollmentRepository enrollmentRepository;
  private final LessonRepository lessonRepository;
  private final LessonProgressRepository lessonProgressRepository;
  private final CourseSectionRepository courseSectionRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final CertificateRepository certificateRepository;
  private final QuizRepository quizRepository;
  private final SectionItemRepository sectionItemRepository;
  private final LiveClassRepository liveClassRepository;
  private final LiveClassSlotRepository liveClassSlotRepository;
  private final CourseCompletionService courseCompletionService;
  private final AssetUrlService assetUrlService;
  private final LocalizedContentService localizedContentService;
  private final CurriculumAccessService curriculumAccessService;

  public StudentCourseHomeResponse execute(UUID courseId, Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    Enrollment enrollment =
        enrollmentRepository
            .findByUserIdAndCourseId(userId, courseId)
            .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found or not enrolled"));

    Course course = enrollment.getCourse();
    List<CourseSection> sections =
        courseSectionRepository.findByCourseIdAndStatusOrderByPositionAsc(
            courseId, PublishStatus.PUBLISHED);
    List<Lesson> lessons =
        lessonRepository.findByCourseIdAndStatusWithMediaOrderByPositionAsc(
            courseId, PublishStatus.PUBLISHED);
    List<Quiz> quizzes =
        quizRepository.findByCourseIdAndStatusOrderByPositionAsc(courseId, PublishStatus.PUBLISHED);
    List<LiveClass> liveClasses = liveClassRepository.findByCourseIdOrderByStartsAtAsc(courseId);
    List<SectionItem> sectionItems =
        sectionItemRepository.findBySectionIdInOrderBySectionIdAscPositionAsc(
            sections.stream().map(CourseSection::getId).toList());
    List<LessonProgress> progresses =
        lessonProgressRepository.findByUserIdAndLessonCourseId(userId, courseId);

    Map<UUID, LessonProgress> progressByLessonId = new HashMap<>();
    for (LessonProgress progress : progresses) {
      progressByLessonId.put(progress.getLesson().getId(), progress);
    }

    String instructorName = resolveInstructorName(courseId);
    CourseCompletion courseCompletion = courseCompletionService.get(userId, courseId);
    Set<UUID> passedQuizIds = courseCompletionService.getPassedQuizIds(userId, courseId);
    Set<UUID> attendedLiveClassIds =
        courseCompletionService.getAttendedLiveClassIds(userId, courseId);

    Map<UUID, List<Lesson>> lessonsBySectionId =
        lessons.stream()
            .collect(java.util.stream.Collectors.groupingBy(l -> l.getSection().getId()));
    Map<UUID, List<Quiz>> quizzesBySectionId =
        quizzes.stream()
            .collect(java.util.stream.Collectors.groupingBy(q -> q.getSection().getId()));
    Map<UUID, List<SectionItem>> itemsBySectionId =
        sectionItems.stream()
            .collect(java.util.stream.Collectors.groupingBy(i -> i.getSection().getId()));
    Map<UUID, LiveClass> liveClassBySlotId =
        liveClasses.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    liveClass -> liveClass.getSlot().getId(), value -> value));
    Map<UUID, LiveClassSlot> liveClassSlotById =
        sections.stream()
            .flatMap(section -> liveClassSlotRepository.findBySectionId(section.getId()).stream())
            .collect(java.util.stream.Collectors.toMap(LiveClassSlot::getId, value -> value));

    List<StudentSectionHomeResponse> sectionResponses =
        sections.stream()
            .map(
                section ->
                    toSectionHome(
                        section,
                        lessonsBySectionId.getOrDefault(section.getId(), List.of()),
                        quizzesBySectionId.getOrDefault(section.getId(), List.of()),
                        itemsBySectionId.getOrDefault(section.getId(), List.of()),
                        liveClassBySlotId,
                        liveClassSlotById,
                        progressByLessonId,
                        passedQuizIds,
                        attendedLiveClassIds,
                        enrollment))
            .toList();

    java.util.Optional<Certificate> cert =
        certificateRepository
            .findByUserIdAndCourseId(userId, courseId)
            .filter(c -> c.getRevokedAt() == null);

    return StudentCourseHomeResponse.builder()
        .courseId(course.getId())
        .courseName(localizedContentService.text(course.getTitle(), course.getTitleEn()))
        .courseSlug(course.getSlug())
        .description(
            localizedContentService.text(course.getDescription(), course.getDescriptionEn()))
        .thumbnailUrl(assetUrlService.publicUrl(course.getThumbnailObjectKey()))
        .instructor(instructorName)
        .courseLevel(course.getLevel().name())
        .studyMode(course.getStudyMode())
        .timezone(course.getTimezone())
        .startsAt(course.getStartsAt())
        .endsAt(course.getEndsAt())
        .enrollmentStatus(enrollment.getStatus())
        .enrolledAt(enrollment.getEnrolledAt())
        .expiresAt(enrollment.getExpiresAt())
        .isExpired(
            enrollment.getExpiresAt() != null && !enrollment.getExpiresAt().isAfter(Instant.now()))
        .completionPercentage(courseCompletion.completionPercentage())
        .completedLessons(courseCompletion.completedLessons())
        .totalLessons(courseCompletion.totalLessons())
        .completedLiveClasses(courseCompletion.completedLiveClasses())
        .totalLiveClasses(courseCompletion.totalLiveClasses())
        .completedItems(courseCompletion.completedItems())
        .totalItems(courseCompletion.totalItems())
        .sections(sectionResponses)
        .liveSessions(course.getLiveSessionCount())
        .quizzes(course.getQuizCount())
        .estimatedDurationHours(formatDurationHours(course.getEstimatedDurationMinutes()))
        .hasCertificate(cert.isPresent())
        .certificateCode(cert.map(Certificate::getCertificateCode).orElse(null))
        .build();
  }

  private StudentSectionHomeResponse toSectionHome(
      CourseSection section,
      List<Lesson> lessons,
      List<Quiz> quizzes,
      List<SectionItem> sectionItems,
      Map<UUID, LiveClass> liveClassBySlotId,
      Map<UUID, LiveClassSlot> liveClassSlotById,
      Map<UUID, LessonProgress> progressByLessonId,
      Set<UUID> passedQuizIds,
      Set<UUID> attendedLiveClassIds,
      Enrollment enrollment) {
    Instant now = Instant.now();
    Map<UUID, Integer> positionByItemId =
        sectionItems.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    SectionItem::getItemId, SectionItem::getPosition));
    boolean sectionAccessible =
        curriculumAccessService.isSectionAccessible(section, enrollment, now);
    boolean sectionMandatory = Boolean.TRUE.equals(section.getIsMandatory());
    int totalLessons =
        sectionMandatory
            ? (int) lessons.stream().filter(l -> Boolean.TRUE.equals(l.getIsMandatory())).count()
            : 0;
    int completedLessons =
        sectionMandatory
            ? (int)
                lessons.stream()
                    .filter(lesson -> Boolean.TRUE.equals(lesson.getIsMandatory()))
                    .map(Lesson::getId)
                    .map(progressByLessonId::get)
                    .filter(p -> p != null && p.getCompletedAt() != null)
                    .count()
            : 0;
    int completedQuizzes =
        sectionMandatory
            ? (int) quizzes.stream().map(Quiz::getId).filter(passedQuizIds::contains).count()
            : 0;
    Set<UUID> completableLiveClassIds =
        sectionItems.stream()
            .filter(item -> item.getItemType() == SectionItemType.LIVE_CLASS)
            .map(SectionItem::getItemId)
            .filter(
                id -> {
                  LiveClassSlot slot = liveClassSlotById.get(id);
                  LiveClass scheduled = liveClassBySlotId.get(id);
                  return slot != null
                      && sectionMandatory
                      && Boolean.TRUE.equals(slot.getIsMandatory())
                      && (scheduled == null
                          || (scheduled.getStatus() != LiveClassStatus.CANCELLED
                              && scheduled.getStatus() != LiveClassStatus.FAILED));
                })
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    int totalLiveClasses = completableLiveClassIds.size();
    int completedLiveClasses =
        (int)
            completableLiveClassIds.stream()
                .map(liveClassBySlotId::get)
                .filter(java.util.Objects::nonNull)
                .filter(liveClass -> liveClass.getStatus() == LiveClassStatus.COMPLETED)
                .count();
    int totalItems = totalLessons + (sectionMandatory ? quizzes.size() : 0) + totalLiveClasses;
    int completedItems = completedLessons + completedQuizzes + completedLiveClasses;
    double completion = totalItems == 0 ? 0.0 : (completedItems * 100.0) / totalItems;

    List<StudentLessonHomeResponse> lessonResponses = new java.util.ArrayList<>();
    for (int i = 0; i < lessons.size(); i++) {
      Lesson lesson = lessons.get(i);
      LessonProgress progress = progressByLessonId.get(lesson.getId());
      String prev = i > 0 ? lessons.get(i - 1).getId().toString() : null;
      String next = i < lessons.size() - 1 ? lessons.get(i + 1).getId().toString() : null;

      lessonResponses.add(
          StudentLessonHomeResponse.builder()
              .lessonId(lesson.getId())
              .lessonTitle(localizedContentService.text(lesson.getTitle(), lesson.getTitleEn()))
              .position(positionByItemId.get(lesson.getId()))
              .lessonType(lesson.getLessonType())
              .completed(progress != null && progress.getCompletedAt() != null)
              .completedAt(progress != null ? progress.getCompletedAt() : null)
              .lastPositionSec(progress != null ? progress.getLastPositionSec() : null)
              .isAccessible(
                  sectionAccessible
                      && curriculumAccessService.isReleased(
                          lesson.getReleaseType(),
                          lesson.getReleaseAt(),
                          lesson.getUnlockAfterDays(),
                          enrollment,
                          now))
              .durationLabel(formatLessonDuration(lesson.getDurationSeconds()))
              .isFree(lesson.getIsFree())
              .nextLessonId(next)
              .previousLessonId(prev)
              .build());
    }

    List<StudentQuizHomeResponse> quizResponses =
        quizzes.stream()
            .map(
                quiz ->
                    StudentQuizHomeResponse.builder()
                        .quizId(quiz.getId())
                        .quizTitle(localizedContentService.text(quiz.getTitle(), quiz.getTitleEn()))
                        .position(positionByItemId.get(quiz.getId()))
                        .isAccessible(sectionAccessible)
                        .accessReason(sectionAccessible ? "AVAILABLE" : "SECTION_LOCKED")
                        .passingScorePct(quiz.getPassingScorePct())
                        .maxAttempts(quiz.getMaxAttempts())
                        .timeLimitSec(quiz.getTimeLimitSec())
                        .completed(passedQuizIds.contains(quiz.getId()))
                        .build())
            .toList();
    Map<UUID, StudentLessonHomeResponse> lessonResponseById =
        lessonResponses.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    StudentLessonHomeResponse::lessonId, value -> value));
    Map<UUID, StudentQuizHomeResponse> quizResponseById =
        quizResponses.stream()
            .collect(
                java.util.stream.Collectors.toMap(StudentQuizHomeResponse::quizId, value -> value));
    List<StudentSectionItemResponse> itemResponses =
        sectionItems.stream()
            .map(
                item ->
                    toSectionItemResponse(
                        item,
                        lessonResponseById,
                        quizResponseById,
                        liveClassBySlotId,
                        liveClassSlotById,
                        attendedLiveClassIds))
            .filter(java.util.Objects::nonNull)
            .toList();

    return StudentSectionHomeResponse.builder()
        .sectionId(section.getId())
        .sectionTitle(localizedContentService.text(section.getTitle(), section.getTitleEn()))
        .position(section.getPosition())
        .description(
            localizedContentService.text(section.getDescription(), section.getDescriptionEn()))
        .completionPercentage(round2(completion))
        .completedLessons(completedLessons)
        .totalLessons(totalLessons)
        .completedLiveClasses(completedLiveClasses)
        .totalLiveClasses(totalLiveClasses)
        .completedItems(completedItems)
        .totalItems(totalItems)
        .isAccessible(sectionAccessible)
        .accessReason(sectionAccessible ? "AVAILABLE" : "SECTION_LOCKED")
        .items(itemResponses)
        .lessons(lessonResponses)
        .quizzes(quizResponses)
        .build();
  }

  private StudentSectionItemResponse toSectionItemResponse(
      SectionItem item,
      Map<UUID, StudentLessonHomeResponse> lessonById,
      Map<UUID, StudentQuizHomeResponse> quizById,
      Map<UUID, LiveClass> liveClassBySlotId,
      Map<UUID, LiveClassSlot> liveClassSlotById,
      Set<UUID> attendedLiveClassIds) {
    return switch (item.getItemType()) {
      case LESSON -> {
        StudentLessonHomeResponse lesson = lessonById.get(item.getItemId());
        yield lesson == null
            ? null
            : StudentSectionItemResponse.builder()
                .itemId(item.getItemId())
                .itemType(item.getItemType())
                .position(item.getPosition())
                .lesson(lesson)
                .build();
      }
      case QUIZ -> {
        StudentQuizHomeResponse quiz = quizById.get(item.getItemId());
        yield quiz == null
            ? null
            : StudentSectionItemResponse.builder()
                .itemId(item.getItemId())
                .itemType(item.getItemType())
                .position(item.getPosition())
                .quiz(quiz)
                .build();
      }
      case LIVE_CLASS -> {
        LiveClassSlot slot = liveClassSlotById.get(item.getItemId());
        LiveClass liveClass = liveClassBySlotId.get(item.getItemId());
        yield slot == null
            ? null
            : StudentSectionItemResponse.builder()
                .itemId(item.getItemId())
                .itemType(item.getItemType())
                .position(item.getPosition())
                .liveClass(
                    StudentLiveClassHomeResponse.builder()
                        .liveClassId(liveClass != null ? liveClass.getId() : null)
                        .liveClassItemId(slot.getId())
                        .scheduled(liveClass != null)
                        .title(localizedContentService.text(slot.getTitle(), slot.getTitleEn()))
                        .description(
                            localizedContentService.text(
                                slot.getDescription(), slot.getDescriptionEn()))
                        .startsAt(liveClass != null ? liveClass.getStartsAt() : null)
                        .endsAt(liveClass != null ? liveClass.getEndsAt() : null)
                        .provider(liveClass != null ? liveClass.getProvider() : null)
                        .status(liveClass != null ? liveClass.getStatus() : null)
                        .attended(
                            liveClass != null && attendedLiveClassIds.contains(liveClass.getId()))
                        .completed(
                            liveClass != null && liveClass.getStatus() == LiveClassStatus.COMPLETED)
                        .build())
                .build();
      }
    };
  }

  private String resolveInstructorName(UUID courseId) {
    List<CourseInstructor> instructors = courseInstructorRepository.findByCourseId(courseId);
    return instructors.stream()
        .filter(i -> i.getRole() == InstructorRole.PRIMARY)
        .findFirst()
        .or(() -> instructors.stream().findFirst())
        .map(i -> i.getInstructor().getFullName())
        .orElse("Instructor");
  }

  private String formatLessonDuration(Integer durationSec) {
    if (durationSec == null || durationSec <= 0) {
      return null;
    }
    long minutes = Math.max(1, Duration.ofSeconds(durationSec).toMinutes());
    return minutes + " minutes";
  }

  private String formatDurationHours(Integer minutes) {
    if (minutes == null || minutes <= 0) {
      return null;
    }
    double hours = minutes / 60.0;
    return round2(hours) + " hours";
  }

  private double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
