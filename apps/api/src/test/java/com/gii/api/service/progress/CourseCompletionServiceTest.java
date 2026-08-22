package com.gii.api.service.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import com.gii.common.repository.live.LiveClassAttendanceRepository;
import com.gii.common.repository.live.LiveClassRepository;
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
  @Mock private LiveClassRepository liveClassRepository;
  @Mock private LiveClassAttendanceRepository liveClassAttendanceRepository;

  @InjectMocks private CourseCompletionService service;

  @Test
  void combinesLessonsQuizzesAndCompletedLiveClassesIntoOnePercentage() {
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
    List<LiveClassStatus> liveStatuses =
        List.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE, LiveClassStatus.COMPLETED);
    when(liveClassRepository.countCompletableByCourseIdsAndStatuses(
            courseIds, PublishStatus.PUBLISHED, liveStatuses))
        .thenReturn(List.<Object[]>of(new Object[] {courseId, 2L}));
    when(liveClassRepository.countByCourseIdsAndSectionStatusAndLiveClassStatus(
            courseIds, PublishStatus.PUBLISHED, LiveClassStatus.COMPLETED))
        .thenReturn(List.<Object[]>of(new Object[] {courseId, 1L}));

    var completion = service.get(userId, courseId);

    assertThat(completion.totalLessons()).isEqualTo(3);
    assertThat(completion.completedLessons()).isEqualTo(2);
    assertThat(completion.totalQuizzes()).isEqualTo(2);
    assertThat(completion.completedQuizzes()).isEqualTo(1);
    assertThat(completion.totalLiveClasses()).isEqualTo(2);
    assertThat(completion.completedLiveClasses()).isEqualTo(1);
    assertThat(completion.totalItems()).isEqualTo(7);
    assertThat(completion.completedItems()).isEqualTo(4);
    assertThat(completion.completionPercentage()).isEqualTo(57.14);
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
    List<LiveClassStatus> liveStatuses =
        List.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE, LiveClassStatus.COMPLETED);
    when(liveClassRepository.countCompletableByCourseIdsAndStatuses(
            courseIds, PublishStatus.PUBLISHED, liveStatuses))
        .thenReturn(List.of());
    when(liveClassRepository.countByCourseIdsAndSectionStatusAndLiveClassStatus(
            courseIds, PublishStatus.PUBLISHED, LiveClassStatus.COMPLETED))
        .thenReturn(List.of());

    Map<UUID, CourseCompletionService.CourseCompletion> result =
        service.getByCourseIds(userId, courseIds);

    assertThat(result.get(courseId).totalItems()).isZero();
    assertThat(result.get(courseId).completionPercentage()).isZero();
  }
}
