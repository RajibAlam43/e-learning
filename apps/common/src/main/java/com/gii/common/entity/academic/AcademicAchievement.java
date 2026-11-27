package com.gii.common.entity.academic;

import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.enums.AcademicAchievementSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Immutable academic credit award; course delivery and payment records remain separate. */
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "academic_achievements",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_academic_achievement_user_template",
            columnNames = {"user_id", "course_template_id"}))
public class AcademicAchievement extends BaseUuidEntity {
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "course_template_id", nullable = false, updatable = false)
  private UUID courseTemplateId;

  @Column(name = "source_enrollment_id", updatable = false)
  private UUID sourceEnrollmentId;

  @Column(name = "source_course_id", updatable = false)
  private UUID sourceCourseId;

  @Column(name = "semester_id", updatable = false)
  private UUID semesterId;

  @Column(name = "course_title", nullable = false, updatable = false)
  private String courseTitle;

  @Column(nullable = false, updatable = false)
  private Integer credits;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 30)
  private AcademicAchievementSource source;

  @Column(name = "achieved_at", nullable = false, updatable = false)
  private Instant achievedAt;
}
