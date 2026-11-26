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
@Table(name = "google_meet_oauth_states")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoogleMeetOauthState extends BaseUuidEntity {
  @Column(nullable = false, unique = true)
  private String stateHash;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private String browserHash;

  @Column(nullable = false)
  private String nonceHash;

  @JsonIgnore
  @Column(nullable = false, columnDefinition = "text")
  private String encryptedVerifier;

  @Column(nullable = false, columnDefinition = "text")
  private String requestedScopes;

  @Column(nullable = false)
  private Instant expiresAt;

  private Instant consumedAt;
}
