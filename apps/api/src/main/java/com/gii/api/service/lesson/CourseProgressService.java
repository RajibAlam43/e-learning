package com.gii.api.service.lesson;

import com.gii.api.model.response.lesson.CourseProgressResponse;
import com.gii.api.model.response.lesson.LessonProgressSummaryResponse;
import com.gii.api.model.response.lesson.SectionProgressResponse;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.progress.CourseCompletionService;
import com.gii.api.service.progress.CourseCompletionService.CourseCompletion;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import com.gii.common.repository.quiz.QuizRepository;
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
public class CourseProgressService {

  private final LessonAccessService lessonAccessService;
  private final EnrollmentRepository enrollmentRepository;
  private final LessonRepository lessonRepository;
  private final LessonProgressRepository lessonProgressRepository;
  private final CourseSectionRepository sectionRepository;
  private final QuizRepository quizRepository;
  private final CourseCompletionService courseCompletionService;
  private final LocalizedContentService localizedContentService;

  public CourseProgressResponse execute(UUID courseId, Authentication authentication) {
    UUID userId = lessonAccessService.requireCurrentUserId(authentication);
    Enrollment enrollment =
        enrollmentRepository
            .findByUserIdAndCourseId(userId, courseId)
            .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found or not enrolled"));

    List<Lesson> lessons =
        lessonRepository.findByCourseIdAndStatusWithMediaOrderByPositionAsc(
            courseId, PublishStatus.PUBLISHED);
    List<LessonProgress> progresses =
        lessonProgressRepository.findByUserIdAndLessonCourseId(userId, courseId);
    List<CourseSection> sections =
        sectionRepository.findByCourseIdAndStatusOrderByPositionAsc(
            courseId, PublishStatus.PUBLISHED);
    List<Quiz> quizzes =
        quizRepository.findByCourseIdAndStatusOrderByPositionAsc(courseId, PublishStatus.PUBLISHED);
    Set<UUID> passedQuizIds = courseCompletionService.getPassedQuizIds(userId, courseId);
    CourseCompletion courseCompletion = courseCompletionService.get(userId, courseId);

    Map<UUID, LessonProgress> progressByLessonId = new HashMap<>();
    for (LessonProgress progress : progresses) {
      progressByLessonId.put(progress.getLesson().getId(), progress);
    }

    Map<UUID, List<Lesson>> lessonsBySectionId =
        lessons.stream()
            .collect(java.util.stream.Collectors.groupingBy(lesson -> lesson.getSection().getId()));
    Map<UUID, List<Quiz>> quizzesBySectionId =
        quizzes.stream()
            .collect(java.util.stream.Collectors.groupingBy(quiz -> quiz.getSection().getId()));

    List<SectionProgressResponse> sectionResponses =
        sections.stream()
            .map(
                section -> {
                  List<Lesson> sectionLessons =
                      lessonsBySectionId.getOrDefault(section.getId(), List.of());
                  int sectionTotal = sectionLessons.size();
                  int sectionCompleted =
                      (int)
                          sectionLessons.stream()
                              .map(Lesson::getId)
                              .map(progressByLessonId::get)
                              .filter(
                                  progress -> progress != null && progress.getCompletedAt() != null)
                              .count();
                  List<Quiz> sectionQuizzes =
                      quizzesBySectionId.getOrDefault(section.getId(), List.of());
                  int completedQuizzes =
                      (int)
                          sectionQuizzes.stream()
                              .map(Quiz::getId)
                              .filter(passedQuizIds::contains)
                              .count();
                  int sectionTotalItems = sectionTotal + sectionQuizzes.size();
                  int sectionCompletedItems = sectionCompleted + completedQuizzes;
                  double sectionPct =
                      sectionTotalItems == 0
                          ? 0.0
                          : (sectionCompletedItems * 100.0) / sectionTotalItems;

                  List<LessonProgressSummaryResponse> lessonResponses =
                      sectionLessons.stream()
                          .map(
                              lesson -> {
                                LessonProgress progress = progressByLessonId.get(lesson.getId());
                                return LessonProgressSummaryResponse.builder()
                                    .lessonId(lesson.getId())
                                    .lessonTitle(
                                        localizedContentService.text(
                                            lesson.getTitle(), lesson.getTitleEn()))
                                    .position(lesson.getPosition())
                                    .lessonType(lesson.getLessonType())
                                    .completed(
                                        progress != null && progress.getCompletedAt() != null)
                                    .completedAt(
                                        progress != null ? progress.getCompletedAt() : null)
                                    .lastPositionSec(
                                        progress != null ? progress.getLastPositionSec() : null)
                                    .isAccessible(true)
                                    .build();
                              })
                          .toList();

                  return SectionProgressResponse.builder()
                      .sectionId(section.getId())
                      .sectionTitle(
                          localizedContentService.text(section.getTitle(), section.getTitleEn()))
                      .position(section.getPosition())
                      .totalLessons(sectionTotal)
                      .completedLessons(sectionCompleted)
                      .totalItems(sectionTotalItems)
                      .completedItems(sectionCompletedItems)
                      .completionPercentage(round2(sectionPct))
                      .lessons(lessonResponses)
                      .build();
                })
            .toList();

    return CourseProgressResponse.builder()
        .courseId(enrollment.getCourse().getId())
        .courseName(
            localizedContentService.text(
                enrollment.getCourse().getTitle(), enrollment.getCourse().getTitleEn()))
        .courseSlug(enrollment.getCourse().getSlug())
        .totalLessons(courseCompletion.totalLessons())
        .completedLessons(courseCompletion.completedLessons())
        .pendingLessons(
            Math.max(0, courseCompletion.totalLessons() - courseCompletion.completedLessons()))
        .totalItems(courseCompletion.totalItems())
        .completedItems(courseCompletion.completedItems())
        .pendingItems(
            Math.max(0, courseCompletion.totalItems() - courseCompletion.completedItems()))
        .completionPercentage(courseCompletion.completionPercentage())
        .enrolledAt(enrollment.getEnrolledAt())
        .completedAt(enrollment.getCompletedAt())
        .expiresAt(enrollment.getExpiresAt())
        .sections(sectionResponses)
        .build();
  }

  private double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
