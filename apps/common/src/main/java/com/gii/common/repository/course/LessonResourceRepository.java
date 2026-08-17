package com.gii.common.repository.course;

import com.gii.common.entity.course.LessonResource;
import com.gii.common.enums.LessonResourcePurpose;
import com.gii.common.enums.LessonResourceType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonResourceRepository extends JpaRepository<LessonResource, UUID> {

  List<LessonResource> findByLessonIdOrderByPositionAsc(UUID lessonId);

  Optional<LessonResource> findByLessonIdAndPurpose(UUID lessonId, LessonResourcePurpose purpose);

  boolean existsByLessonIdAndPurposeAndResourceType(
      UUID lessonId, LessonResourcePurpose purpose, LessonResourceType resourceType);

  Optional<LessonResource> findById(UUID id);
}
