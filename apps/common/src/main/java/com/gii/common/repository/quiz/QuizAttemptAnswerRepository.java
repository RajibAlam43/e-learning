package com.gii.common.repository.quiz;

import com.gii.common.entity.quiz.QuizAttemptAnswer;
import com.gii.common.entity.quiz.QuizAttemptAnswerId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptAnswerRepository
    extends JpaRepository<QuizAttemptAnswer, QuizAttemptAnswerId> {

  List<QuizAttemptAnswer> findByAttemptId(UUID attemptId);

  void deleteByAttemptId(UUID attemptId);
}
