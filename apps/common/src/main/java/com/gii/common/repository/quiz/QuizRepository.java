package com.gii.common.repository.quiz;

import com.gii.common.entity.quiz.Quiz;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

  List<Quiz> findByCourseId(UUID courseId);

  Optional<Quiz> findByLessonId(UUID lessonId);

  Optional<Quiz> findByCourseIdAndLessonIsNull(UUID courseId);

  Optional<Quiz> findByIdAndStatus(UUID id, PublishStatus status);
}
