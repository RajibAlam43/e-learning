package com.gii.common.repository.quiz;

import com.gii.common.entity.quiz.QuizQuestion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

  List<QuizQuestion> findByQuizIdOrderByPositionAsc(UUID quizId);

  @Query(
      """
        SELECT question.quiz.id, COUNT(question)
        FROM QuizQuestion question
        WHERE question.quiz.id IN :quizIds
        GROUP BY question.quiz.id
      """)
  List<Object[]> countByQuizIds(@Param("quizIds") List<UUID> quizIds);
}
