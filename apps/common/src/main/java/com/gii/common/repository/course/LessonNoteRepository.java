package com.gii.common.repository.course;

import com.gii.common.entity.course.LessonNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LessonNoteRepository extends JpaRepository<LessonNote, UUID> {

    Optional<LessonNote> findByLessonId(UUID lessonId);
}