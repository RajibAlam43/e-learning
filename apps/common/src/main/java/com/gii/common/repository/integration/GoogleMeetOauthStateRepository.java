package com.gii.common.repository.integration;

import com.gii.common.entity.integration.GoogleMeetOauthState;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoogleMeetOauthStateRepository extends JpaRepository<GoogleMeetOauthState, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM GoogleMeetOauthState s WHERE s.stateHash = :hash")
  Optional<GoogleMeetOauthState> findLocked(@Param("hash") String hash);

  void deleteByExpiresAtBefore(Instant expiresAt);
}
