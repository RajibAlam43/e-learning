package com.gii.common.entity.enrollment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "student_learning_streaks")
public class StudentLearningStreak {

  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "current_streak", nullable = false)
  private Integer currentStreak;

  @Column(name = "max_streak", nullable = false)
  private Integer maxStreak;

  @Column(name = "last_activity_date", nullable = false)
  private LocalDate lastActivityDate;

  @Column(name = "last_activity_at", nullable = false)
  private Instant lastActivityAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
