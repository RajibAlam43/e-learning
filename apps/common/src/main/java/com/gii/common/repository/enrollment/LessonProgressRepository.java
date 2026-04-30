package com.gii.common.repository.enrollment;

import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.enrollment.LessonProgressId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, LessonProgressId> {

  List<LessonProgress> findByUserId(UUID userId);

  List<LessonProgress> findByUserIdAndLessonCourseId(UUID userId, UUID courseId);

  long countByUserIdAndLessonCourseIdAndCompletedAtIsNotNull(UUID userId, UUID courseId);
}
