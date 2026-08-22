package com.gii.common.repository.enrollment;

import com.gii.common.entity.enrollment.StudentLearningStreak;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentLearningStreakRepository
    extends JpaRepository<StudentLearningStreak, UUID> {

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
            INSERT INTO student_learning_streaks AS streak (
              user_id,
              current_streak,
              max_streak,
              last_activity_date,
              last_activity_at,
              updated_at
            ) VALUES (
              :userId,
              1,
              1,
              :activityDate,
              :activityAt,
              now()
            )
            ON CONFLICT (user_id) DO UPDATE SET
              current_streak = CASE
                WHEN EXCLUDED.last_activity_date <= streak.last_activity_date
                  THEN streak.current_streak
                WHEN EXCLUDED.last_activity_date = streak.last_activity_date + 1
                  THEN streak.current_streak + 1
                ELSE 1
              END,
              max_streak = GREATEST(
                streak.max_streak,
                CASE
                  WHEN EXCLUDED.last_activity_date <= streak.last_activity_date
                    THEN streak.current_streak
                  WHEN EXCLUDED.last_activity_date = streak.last_activity_date + 1
                    THEN streak.current_streak + 1
                  ELSE 1
                END
              ),
              last_activity_date = GREATEST(
                streak.last_activity_date,
                EXCLUDED.last_activity_date
              ),
              last_activity_at = GREATEST(
                streak.last_activity_at,
                EXCLUDED.last_activity_at
              ),
              updated_at = now()
          """,
      nativeQuery = true)
  void recordActivity(
      @Param("userId") UUID userId,
      @Param("activityDate") LocalDate activityDate,
      @Param("activityAt") Instant activityAt);
}
