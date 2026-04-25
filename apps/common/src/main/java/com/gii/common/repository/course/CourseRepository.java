package com.gii.common.repository.course;

import com.gii.common.model.course.Course;
import com.gii.common.model.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    Page<Course> findByCategoryIdAndStatus(UUID categoryId, CourseStatus status, Pageable pageable);

    Page<Course> findByCreatedById(UUID createdById, Pageable pageable);

    long countByStatus(CourseStatus status);
}