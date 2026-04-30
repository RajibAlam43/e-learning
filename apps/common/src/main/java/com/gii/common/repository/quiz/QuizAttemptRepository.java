package com.gii.common.repository.quiz;

import com.gii.common.entity.quiz.QuizAttempt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

  List<QuizAttempt> findByQuizIdAndUserIdOrderByAttemptNoDesc(UUID quizId, UUID userId);

  Optional<QuizAttempt> findByIdAndUserId(UUID id, UUID userId);

  long countByQuizIdAndUserId(UUID quizId, UUID userId);
}
