package com.gii.common.repository.quiz;

import com.gii.common.entity.quiz.Quiz;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

  Optional<Quiz> findByIdAndStatus(UUID id, PublishStatus status);

  List<Quiz> findBySectionIdOrderByPositionAsc(UUID sectionId);

  boolean existsBySectionIdAndPosition(UUID sectionId, Integer position);
}
