package com.gii.api.service.progress;

import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import com.gii.common.repository.live.LiveClassAttendanceRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.quiz.QuizAttemptRepository;
import com.gii.common.repository.quiz.QuizRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCompletionService {

  private static final List<LiveClassStatus> COMPLETABLE_LIVE_CLASS_STATUSES =
      List.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE, LiveClassStatus.COMPLETED);

  private final LessonRepository lessonRepository;
  private final LessonProgressRepository lessonProgressRepository;
  private final QuizRepository quizRepository;
  private final QuizAttemptRepository quizAttemptRepository;
  private final LiveClassRepository liveClassRepository;
  private final LiveClassAttendanceRepository liveClassAttendanceRepository;

  public Map<UUID, CourseCompletion> getByCourseIds(UUID userId, List<UUID> courseIds) {
    if (courseIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, Integer> totalLessons =
        toCountMap(
            lessonRepository.countCompletableByCourseIdsAndStatus(
                courseIds, PublishStatus.PUBLISHED));
    Map<UUID, Integer> completedLessons =
        toCountMap(
            lessonProgressRepository.countCompletedPublishedByUserIdAndCourseIds(
                userId, courseIds, PublishStatus.PUBLISHED));
    Map<UUID, Integer> totalQuizzes =
        toCountMap(quizRepository.countByCourseIdsAndStatus(courseIds, PublishStatus.PUBLISHED));
    Map<UUID, Integer> completedQuizzes =
        toCountMap(
            quizAttemptRepository.countPassedQuizzesByUserIdAndCourseIds(
                userId, courseIds, PublishStatus.PUBLISHED));
    Map<UUID, Integer> totalLiveClasses =
        toCountMap(
            liveClassRepository.countCompletableByCourseIdsAndStatuses(
                courseIds, PublishStatus.PUBLISHED, COMPLETABLE_LIVE_CLASS_STATUSES));
    Map<UUID, Integer> completedLiveClasses =
        toCountMap(
            liveClassRepository.countByCourseIdsAndSectionStatusAndLiveClassStatus(
                courseIds, PublishStatus.PUBLISHED, LiveClassStatus.COMPLETED));

    Map<UUID, CourseCompletion> result = new HashMap<>();
    for (UUID courseId : courseIds) {
      int lessons = totalLessons.getOrDefault(courseId, 0);
      int completedLessonCount = completedLessons.getOrDefault(courseId, 0);
      int quizzes = totalQuizzes.getOrDefault(courseId, 0);
      int completedQuizCount = completedQuizzes.getOrDefault(courseId, 0);
      int liveClasses = totalLiveClasses.getOrDefault(courseId, 0);
      int completedLiveClassCount = completedLiveClasses.getOrDefault(courseId, 0);
      result.put(
          courseId,
          CourseCompletion.create(
              lessons,
              completedLessonCount,
              quizzes,
              completedQuizCount,
              liveClasses,
              completedLiveClassCount));
    }
    return result;
  }

  public CourseCompletion get(UUID userId, UUID courseId) {
    return getByCourseIds(userId, List.of(courseId)).get(courseId);
  }

  public Set<UUID> getPassedQuizIds(UUID userId, UUID courseId) {
    return quizAttemptRepository
        .findPassedQuizIds(userId, courseId, PublishStatus.PUBLISHED)
        .stream()
        .collect(Collectors.toUnmodifiableSet());
  }

  public Set<UUID> getAttendedLiveClassIds(UUID userId, UUID courseId) {
    return liveClassAttendanceRepository
        .findAttendedCompletableLiveClassIds(
            userId, courseId, PublishStatus.PUBLISHED, COMPLETABLE_LIVE_CLASS_STATUSES)
        .stream()
        .collect(Collectors.toUnmodifiableSet());
  }

  private Map<UUID, Integer> toCountMap(List<Object[]> rows) {
    Map<UUID, Integer> result = new HashMap<>();
    for (Object[] row : rows) {
      result.put((UUID) row[0], ((Number) row[1]).intValue());
    }
    return result;
  }

  public record CourseCompletion(
      int totalLessons,
      int completedLessons,
      int totalQuizzes,
      int completedQuizzes,
      int totalLiveClasses,
      int completedLiveClasses,
      int totalItems,
      int completedItems,
      double completionPercentage) {

    static CourseCompletion create(
        int totalLessons,
        int completedLessons,
        int totalQuizzes,
        int completedQuizzes,
        int totalLiveClasses,
        int completedLiveClasses) {
      int totalItems = totalLessons + totalQuizzes + totalLiveClasses;
      int completedItems = completedLessons + completedQuizzes + completedLiveClasses;
      double percentage =
          totalItems == 0 ? 0.0 : Math.round(completedItems * 10000.0 / totalItems) / 100.0;
      return new CourseCompletion(
          totalLessons,
          completedLessons,
          totalQuizzes,
          completedQuizzes,
          totalLiveClasses,
          completedLiveClasses,
          totalItems,
          completedItems,
          percentage);
    }
  }
}
