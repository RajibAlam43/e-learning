package com.gii.common.repository.quiz;

import com.gii.common.model.quiz.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    List<QuizAttempt> findByQuizIdAndUserIdOrderByAttemptNoDesc(UUID quizId, UUID userId);

    Optional<QuizAttempt> findTopByQuizIdAndUserIdOrderByAttemptNoDesc(UUID quizId, UUID userId);

    long countByQuizIdAndUserId(UUID quizId, UUID userId);

    boolean existsByQuizIdAndUserIdAndPassedTrue(UUID quizId, UUID userId);
}