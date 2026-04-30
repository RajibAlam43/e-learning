package com.gii.common.repository.course;

import com.gii.common.entity.course.Course;
import com.gii.common.enums.PublishStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseRepository
    extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {

  Optional<Course> findBySlugAndStatus(String slug, PublishStatus status);
}
