package com.gii.common.entity.academic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.ProgramRegistrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "program_course_registrations")
public class ProgramCourseRegistration extends BaseUuidEntity {
  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "collection_enrollment_id", nullable = false, updatable = false)
  private CollectionEnrollment programEnrollment;

  @Column(nullable = false, updatable = false)
  private UUID courseTemplateId;

  @Column(name = "course_offering_id", nullable = false, updatable = false)
  private UUID courseId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "enrollment_id", nullable = false, updatable = false)
  private Enrollment enrollment;

  @Column(nullable = false, updatable = false)
  private Integer credits;

  @Column(nullable = false, updatable = false)
  private boolean mandatory;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProgramRegistrationStatus status;

  @Column(updatable = false)
  private Instant accessExpiresAt;

  private Instant withdrawnAt;
  private UUID withdrawnBy;
  private String withdrawalReason;
}
