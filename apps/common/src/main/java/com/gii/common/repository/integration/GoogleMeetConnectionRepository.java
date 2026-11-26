package com.gii.common.repository.integration;

import com.gii.common.entity.integration.GoogleMeetConnection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleMeetConnectionRepository extends JpaRepository<GoogleMeetConnection, UUID> {
  Optional<GoogleMeetConnection> findByUserId(UUID userId);

  void deleteByUserId(UUID userId);
}
