package com.gii.common.repository.live;

import com.gii.common.entity.live.LiveClassRegistrant;
import com.gii.common.enums.LiveClassRegistrantStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LiveClassRegistrantRepository extends JpaRepository<LiveClassRegistrant, UUID> {

  Optional<LiveClassRegistrant> findByLiveClassIdAndUserId(UUID liveClassId, UUID userId);

  List<LiveClassRegistrant> findByLiveClassIdOrderByCreatedAtAsc(UUID liveClassId);

  @Query(
      """
        SELECT r FROM LiveClassRegistrant r
        WHERE r.user.id = :userId
        AND r.liveClass.id IN :liveClassIds
      """)
  List<LiveClassRegistrant> findByUserIdAndLiveClassIds(UUID userId, List<UUID> liveClassIds);

  long countByLiveClassIdAndStatus(UUID liveClassId, LiveClassRegistrantStatus status);
}
