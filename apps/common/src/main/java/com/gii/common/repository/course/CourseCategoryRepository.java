package com.gii.common.repository.course;

import com.gii.common.entity.course.CourseCategory;
import com.gii.common.entity.course.CourseCategoryId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, CourseCategoryId> {

  @Query("SELECT cc FROM CourseCategory cc JOIN FETCH cc.category WHERE cc.course.id = :courseId")
  List<CourseCategory> findByCourseId(@Param("courseId") UUID courseId);

  @Query("SELECT cc FROM CourseCategory cc JOIN FETCH cc.category WHERE cc.course.id IN :courseIds")
  List<CourseCategory> findByCourseIds(@Param("courseIds") List<UUID> courseIds);
}
