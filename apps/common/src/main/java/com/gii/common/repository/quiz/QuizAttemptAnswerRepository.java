package com.gii.common.repository.quiz;

import com.gii.common.entity.quiz.QuizAttemptAnswer;
import com.gii.common.entity.quiz.QuizAttemptAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, QuizAttemptAnswerId> {

    List<QuizAttemptAnswer> findByAttemptId(UUID attemptId);
}