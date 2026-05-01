package com.gii.common.repository.quiz;

import com.gii.common.entity.quiz.QuizChoice;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizChoiceRepository extends JpaRepository<QuizChoice, UUID> {

  List<QuizChoice> findByQuestionId(UUID questionId);

  List<QuizChoice> findByQuestionIdIn(List<UUID> questionIds);
}
