package com.gii.common.repository.enrollment;

import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.enrollment.LessonProgressId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, LessonProgressId> {

    List<LessonProgress> findByUserId(UUID userId);

    List<LessonProgress> findByUserIdAndLessonCourseId(UUID userId, UUID courseId);

    long countByUserIdAndLessonCourseIdAndCompletedTrue(UUID userId, UUID courseId);
}