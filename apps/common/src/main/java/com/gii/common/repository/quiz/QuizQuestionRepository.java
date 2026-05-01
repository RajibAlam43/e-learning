package com.gii.common.repository.quiz;

import com.gii.common.entity.quiz.QuizQuestion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

  List<QuizQuestion> findByQuizIdOrderByPositionAsc(UUID quizId);
}
