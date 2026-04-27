package com.gii.common.repository.course;

import com.gii.common.entity.course.Course;
import com.gii.common.enums.PublishStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {

    Optional<Course> findBySlugAndStatus(String slug, PublishStatus status);

    Page<Course> findByStatus(PublishStatus status, Pageable pageable);
}