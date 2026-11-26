package com.gii.common.entity.academic;

import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.enums.WaiverStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "course_waiver_requests")
public class CourseWaiverRequest extends BaseUuidEntity {
  @Column(nullable = false, updatable = false)
  private UUID userId;

  @Column(nullable = false, updatable = false)
  private UUID courseTemplateId;

  @Column(nullable = false, updatable = false)
  private String courseTitle;

  @Column(nullable = false, updatable = false)
  private Integer requestedCredits;

  @Column(nullable = false, updatable = false)
  private String reason;

  @Column(updatable = false)
  private String evidenceObjectKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private WaiverStatus status;

  private String reviewReason;
  private UUID reviewerId;
  private Instant reviewedAt;
  private UUID achievementId;
}
