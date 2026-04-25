package com.gii.common.repository.course;

import com.gii.common.model.course.Lesson;
import com.gii.common.model.enums.LessonStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    Optional<Lesson> findByCourseIdAndSlug(UUID courseId, String slug);

    List<Lesson> findByCourseIdOrderByPositionAsc(UUID courseId);

    List<Lesson> findByCourseIdAndStatusOrderByPositionAsc(UUID courseId, LessonStatus status);

    long countByCourseIdAndStatus(UUID courseId, LessonStatus status);
}