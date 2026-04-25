package com.gii.common.repository.course;

import com.gii.common.model.course.CourseInstructor;
import com.gii.common.model.course.CourseInstructorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseInstructorRepository extends JpaRepository<CourseInstructor, CourseInstructorId> {

    List<CourseInstructor> findByCourseId(UUID courseId);

    List<CourseInstructor> findByInstructorId(UUID instructorUserId);

    boolean existsByCourseIdAndInstructorId(UUID courseId, UUID instructorUserId);
}