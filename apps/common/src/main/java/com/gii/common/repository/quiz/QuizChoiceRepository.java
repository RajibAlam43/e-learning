package com.gii.common.repository.quiz;

import com.gii.common.entity.quiz.QuizChoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizChoiceRepository extends JpaRepository<QuizChoice, UUID> {

    List<QuizChoice> findByQuestionId(UUID questionId);
}