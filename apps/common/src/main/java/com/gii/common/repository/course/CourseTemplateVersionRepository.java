package com.gii.common.repository.course;

import com.gii.common.entity.course.CourseTemplateVersion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseTemplateVersionRepository
    extends JpaRepository<CourseTemplateVersion, UUID> {

  Optional<CourseTemplateVersion> findTopByCourseTemplateIdOrderByVersionNumberDesc(
      UUID courseTemplateId);
}
