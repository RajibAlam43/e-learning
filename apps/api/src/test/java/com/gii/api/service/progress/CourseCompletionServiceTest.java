package com.gii.api.service.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import com.gii.common.repository.quiz.QuizAttemptRepository;
import com.gii.common.repository.quiz.QuizRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseCompletionServiceTest {

  @Mock private LessonRepository lessonRepository;
  @Mock private LessonProgressRepository lessonProgressRepository;
  @Mock private QuizRepository quizRepository;
  @Mock private QuizAttemptRepository quizAttemptRepository;

  @InjectMocks private CourseCompletionService service;

  @Test
  void combinesCompletedLessonsAndPassedQuizzesIntoOnePercentage() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    List<UUID> courseIds = List.of(courseId);

    when(lessonRepository.countCompletableByCourseIdsAndStatus(courseIds, PublishStatus.PUBLISHED))
        .thenReturn(List.<Object[]>of(new Object[] {courseId, 3L}));
    when(lessonProgressRepository.countCompletedPublishedByUserIdAndCourseIds(
            userId, courseIds, PublishStatus.PUBLISHED))
        .thenReturn(List.<Object[]>of(new Object[] {courseId, 2L}));
    when(quizRepository.countByCourseIdsAndStatus(courseIds, PublishStatus.PUBLISHED))
        .thenReturn(List.<Object[]>of(new Object[] {courseId, 2L}));
    when(quizAttemptRepository.countPassedQuizzesByUserIdAndCourseIds(
            userId, courseIds, PublishStatus.PUBLISHED))
        .thenReturn(List.<Object[]>of(new Object[] {courseId, 1L}));

    var completion = service.get(userId, courseId);

    assertThat(completion.totalLessons()).isEqualTo(3);
    assertThat(completion.completedLessons()).isEqualTo(2);
    assertThat(completion.totalQuizzes()).isEqualTo(2);
    assertThat(completion.completedQuizzes()).isEqualTo(1);
    assertThat(completion.totalItems()).isEqualTo(5);
    assertThat(completion.completedItems()).isEqualTo(3);
    assertThat(completion.completionPercentage()).isEqualTo(60.0);
  }

  @Test
  void returnsZeroCompletionForCoursesWithoutPublishedItems() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    List<UUID> courseIds = List.of(courseId);
    when(lessonRepository.countCompletableByCourseIdsAndStatus(courseIds, PublishStatus.PUBLISHED))
        .thenReturn(List.of());
    when(lessonProgressRepository.countCompletedPublishedByUserIdAndCourseIds(
            userId, courseIds, PublishStatus.PUBLISHED))
        .thenReturn(List.of());
    when(quizRepository.countByCourseIdsAndStatus(courseIds, PublishStatus.PUBLISHED))
        .thenReturn(List.of());
    when(quizAttemptRepository.countPassedQuizzesByUserIdAndCourseIds(
            userId, courseIds, PublishStatus.PUBLISHED))
        .thenReturn(List.of());

    Map<UUID, CourseCompletionService.CourseCompletion> result =
        service.getByCourseIds(userId, courseIds);

    assertThat(result.get(courseId).totalItems()).isZero();
    assertThat(result.get(courseId).completionPercentage()).isZero();
  }
}
