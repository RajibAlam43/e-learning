package com.gii.common.entity.live;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.CreatedOnlyUuidEntity;
import com.gii.common.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "live_class_attendance")
public class LiveClassAttendance extends CreatedOnlyUuidEntity {

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "live_class_id", nullable = false)
  private LiveClass liveClass;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "zoom_participant_id")
  private String zoomParticipantId;

  @Column(name = "participant_name")
  private String participantName;

  @Column(name = "participant_email", columnDefinition = "citext")
  private String participantEmail;

  @Column(name = "joined_at")
  private Instant joinedAt;

  @Column(name = "left_at")
  private Instant leftAt;

  @Column(name = "duration_sec")
  private Integer durationSec;
}
