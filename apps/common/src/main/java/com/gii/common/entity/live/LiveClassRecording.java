package com.gii.common.entity.live;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.enums.RecordingMethod;
import com.gii.common.enums.RecordingStatus;
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

@Entity
@Table(name = "live_class_recordings")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LiveClassRecording extends BaseUuidEntity {
  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "live_class_id", nullable = false)
  private LiveClass liveClass;

  @Column(nullable = false)
  private UUID ownerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RecordingMethod method;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RecordingStatus status;

  @Column(nullable = false)
  private String mimeType;

  @Column(nullable = false, unique = true)
  private String objectPrefix;

  private String uploadId;
  private String uploadTokenHash;
  private Instant uploadExpiresAt;
  private Long sizeBytes;
  private Double durationSeconds;
  private Instant publishedAt;
  private Instant processingUntil;
  private UUID processingClaim;
  private int attempts;
  private String failureMessage;
}
