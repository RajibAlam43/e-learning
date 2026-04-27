package com.gii.common.repository.course;

import com.gii.common.entity.course.CourseSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseSectionRepository extends JpaRepository<CourseSection, UUID> {

    List<CourseSection> findByCourseIdOrderByPositionAsc(UUID courseId);
}