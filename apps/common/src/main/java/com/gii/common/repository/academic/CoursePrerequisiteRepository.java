package com.gii.common.repository.academic;

import com.gii.common.entity.academic.CoursePrerequisite;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoursePrerequisiteRepository extends JpaRepository<CoursePrerequisite, UUID> {
  List<CoursePrerequisite> findByCourseTemplateId(UUID templateId);

  void deleteByCourseTemplateId(UUID templateId);
}
