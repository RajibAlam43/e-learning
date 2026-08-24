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

  List<QuizAttempt> findByQuizIdAndEnrollmentIdOrderByAttemptNoDesc(UUID quizId, UUID enrollmentId);

  Optional<QuizAttempt> findByIdAndUserId(UUID id, UUID userId);

  long countByQuizIdAndEnrollmentId(UUID quizId, UUID enrollmentId);

  boolean existsByQuizId(UUID quizId);

  @Query(
      """
        SELECT qa.enrollment.course.id, COUNT(DISTINCT qa.quiz.id)
        FROM QuizAttempt qa
        WHERE qa.user.id = :userId
        AND qa.enrollment.course.id IN :courseIds
        AND qa.quiz.status = :status
        AND qa.quiz.section.status = :status
        AND qa.passed = true
        GROUP BY qa.enrollment.course.id
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
        AND qa.enrollment.course.id = :courseId
        AND qa.quiz.status = :status
        AND qa.quiz.section.status = :status
        AND qa.passed = true
      """)
  List<UUID> findPassedQuizIds(
      @Param("userId") UUID userId,
      @Param("courseId") UUID courseId,
      @Param("status") PublishStatus status);
}
