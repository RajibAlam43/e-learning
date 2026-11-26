package com.gii.common.repository.academic;

import com.gii.common.entity.academic.AcademicAchievement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcademicAchievementRepository extends JpaRepository<AcademicAchievement, UUID> {
  List<AcademicAchievement> findByUserIdOrderByAchievedAtDesc(UUID userId);

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO academic_achievements
            (user_id, course_template_id, source_enrollment_id, source_course_id, semester_id,
             course_title, credits, source, achieved_at)
          SELECT e.user_id, c.course_template_id, e.id, c.id, c.semester_id,
                 COALESCE(NULLIF(BTRIM(t.title_en), ''), t.title), t.credits, 'COMPLETION', e.completed_at
          FROM enrollments e
          JOIN course_offerings c ON c.id = e.course_offering_id
          JOIN course_templates t ON t.id = c.course_template_id
          WHERE e.id = :enrollmentId AND e.completed_at IS NOT NULL
          ON CONFLICT (user_id, course_template_id) DO NOTHING
          """,
      nativeQuery = true)
  int awardCompletion(@Param("enrollmentId") UUID enrollmentId);
}
