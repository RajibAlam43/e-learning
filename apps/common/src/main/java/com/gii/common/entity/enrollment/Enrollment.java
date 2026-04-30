package com.gii.common.entity.enrollment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.CreatedOnlyUuidEntity;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "enrollments",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_enrollments_user_course",
          columnNames = {"user_id", "course_id"})
    })
public class Enrollment extends CreatedOnlyUuidEntity {

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

  @Column(name = "enrolled_at", nullable = false)
  private Instant enrolledAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "expires_at")
  private Instant expiresAt;
}
