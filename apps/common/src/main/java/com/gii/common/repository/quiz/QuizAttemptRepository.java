package com.gii.common.repository.quiz;

import com.gii.common.entity.quiz.QuizAttempt;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

  List<QuizAttempt> findByQuizIdAndUserIdOrderByAttemptNoDesc(UUID quizId, UUID userId);

  Optional<QuizAttempt> findByIdAndUserId(UUID id, UUID userId);

  long countByQuizIdAndUserId(UUID quizId, UUID userId);

  boolean existsByQuizId(UUID quizId);

  @Query(
      """
        SELECT qa.quiz.course.id, COUNT(DISTINCT qa.quiz.id)
        FROM QuizAttempt qa
        WHERE qa.user.id = :userId
        AND qa.quiz.course.id IN :courseIds
        AND qa.quiz.status = :status
        AND qa.quiz.section.status = :status
        AND qa.passed = true
        GROUP BY qa.quiz.course.id
      """)
  List<Object[]> countPassedQuizzesByUserIdAndCourseIds(
      @Param("userId") UUID userId,
      @Param("courseIds") List<UUID> courseIds,
      @Param("status") PublishStatus status);

  @Query(
      """
        SELECT DISTINCT qa.quiz.id
        FROM QuizAttempt qa
        WHERE qa.user.id = :userId
        AND qa.quiz.course.id = :courseId
        AND qa.quiz.status = :status
        AND qa.quiz.section.status = :status
        AND qa.passed = true
      """)
  List<UUID> findPassedQuizIds(
      @Param("userId") UUID userId,
      @Param("courseId") UUID courseId,
      @Param("status") PublishStatus status);
}
