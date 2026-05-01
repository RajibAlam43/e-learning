package com.gii.api.service.lesson;

import com.gii.api.model.response.lesson.CourseProgressResponse;
import com.gii.api.model.response.lesson.LessonProgressSummaryResponse;
import com.gii.api.model.response.lesson.SectionProgressResponse;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    Map<UUID, LessonProgress> progressByLessonId = new HashMap<>();
    for (LessonProgress progress : progresses) {
      progressByLessonId.put(progress.getLesson().getId(), progress);
    }

    Map<UUID, List<Lesson>> lessonsBySectionId =
        lessons.stream()
            .collect(java.util.stream.Collectors.groupingBy(lesson -> lesson.getSection().getId()));

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
                  double sectionPct =
                      sectionTotal == 0 ? 0.0 : (sectionCompleted * 100.0) / sectionTotal;

                  List<LessonProgressSummaryResponse> lessonResponses =
                      sectionLessons.stream()
                          .map(
                              lesson -> {
                                LessonProgress progress = progressByLessonId.get(lesson.getId());
                                return LessonProgressSummaryResponse.builder()
                                    .lessonId(lesson.getId())
                                    .lessonTitle(lesson.getTitle())
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
                      .sectionTitle(section.getTitle())
                      .position(section.getPosition())
                      .totalLessons(sectionTotal)
                      .completedLessons(sectionCompleted)
                      .completionPercentage(round2(sectionPct))
                      .lessons(lessonResponses)
                      .build();
                })
            .toList();

    int totalLessons = lessons.size();
    int completedLessons =
        (int) progresses.stream().filter(progress -> progress.getCompletedAt() != null).count();
    double completionPct = totalLessons == 0 ? 0.0 : (completedLessons * 100.0) / totalLessons;

    return CourseProgressResponse.builder()
        .courseId(enrollment.getCourse().getId())
        .courseName(enrollment.getCourse().getTitle())
        .courseSlug(enrollment.getCourse().getSlug())
        .totalLessons(totalLessons)
        .completedLessons(completedLessons)
        .pendingLessons(Math.max(0, totalLessons - completedLessons))
        .completionPercentage(round2(completionPct))
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
