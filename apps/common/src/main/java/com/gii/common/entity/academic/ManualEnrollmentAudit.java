package com.gii.common.entity.academic;

import com.gii.common.entity.common.CreatedOnlyUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Append-only record of an administrator's enrollment action and exceptions. */
@Entity
@Table(name = "manual_enrollment_audit")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManualEnrollmentAudit extends CreatedOnlyUuidEntity {
  @Column(nullable = false, unique = true)
  private UUID requestId;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private UUID actorId;

  private UUID courseId;
  private UUID programId;

  @Column(nullable = false)
  private boolean programFunded;

  @Column(nullable = false)
  private boolean overrideDates;

  @Column(nullable = false)
  private boolean overrideCapacity;

  @Column(nullable = false)
  private boolean allowUnpublished;

  @Column(nullable = false, columnDefinition = "text")
  private String reason;

  @Column(nullable = false)
  private UUID enrollmentId;

  private UUID registrationId;
  private Instant accessExpiresAt;
  private Instant requestedExpiresAt;

  @Column(nullable = false)
  private boolean changed;
}
