package com.gii.common.repository.course;

import com.gii.common.entity.course.CourseTemplate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseTemplateRepository extends JpaRepository<CourseTemplate, UUID> {}
