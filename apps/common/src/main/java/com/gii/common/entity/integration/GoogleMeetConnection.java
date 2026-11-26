package com.gii.common.entity.integration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "google_meet_connections")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoogleMeetConnection extends BaseUuidEntity {
  @Column(nullable = false, unique = true)
  private UUID userId;

  @Column(nullable = false)
  private String googleSubject;

  @Column(nullable = false)
  private String email;

  @JsonIgnore
  @Column(nullable = false, columnDefinition = "text")
  private String encryptedRefreshToken;

  @Column(nullable = false, columnDefinition = "text")
  private String grantedScopes;

  private Instant refreshExpiresAt;
}
