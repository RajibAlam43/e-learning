package com.gii.common.entity.live;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.entity.user.User;
import com.gii.common.enums.LiveClassRegistrantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
    name = "live_class_registrants",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_live_class_registrant_user",
          columnNames = {"live_class_id", "user_id"})
    })
public class LiveClassRegistrant extends BaseUuidEntity {

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "live_class_id", nullable = false)
  private LiveClass liveClass;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "zoom_registrant_id")
  private String zoomRegistrantId;

  @Column(name = "zoom_join_url")
  private String zoomJoinUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private LiveClassRegistrantStatus status;
}
