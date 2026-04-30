package com.gii.common.repository.course;

import com.gii.common.entity.course.LessonResource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonResourceRepository extends JpaRepository<LessonResource, UUID> {

  List<LessonResource> findByLessonIdOrderByPositionAsc(UUID lessonId);

  Optional<LessonResource> findById(UUID id);
}
