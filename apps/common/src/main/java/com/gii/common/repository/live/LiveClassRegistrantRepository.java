package com.gii.common.repository.live;

import com.gii.common.entity.live.LiveClassRegistrant;
import com.gii.common.enums.LiveClassRegistrantStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveClassRegistrantRepository extends JpaRepository<LiveClassRegistrant, UUID> {

  Optional<LiveClassRegistrant> findByLiveClassIdAndUserIdAndStatus(
      UUID liveClassId, UUID userId, LiveClassRegistrantStatus status);

  List<LiveClassRegistrant> findByLiveClassIdOrderByCreatedAtAsc(UUID liveClassId);

  @Query(
      """
        SELECT r FROM LiveClassRegistrant r
        WHERE r.user.id = :userId
        AND r.liveClass.id IN :liveClassIds
      """)
  List<LiveClassRegistrant> findByUserIdAndLiveClassIds(
      @Param("userId") UUID userId, @Param("liveClassIds") List<UUID> liveClassIds);

  long countByLiveClassIdAndStatus(UUID liveClassId, LiveClassRegistrantStatus status);

  @Query(
      """
        SELECT r.liveClass.id, COUNT(r)
        FROM LiveClassRegistrant r
        WHERE r.liveClass.id IN :liveClassIds
        AND r.status = :status
        GROUP BY r.liveClass.id
      """)
  List<Object[]> countByLiveClassIdsAndStatus(
      @Param("liveClassIds") List<UUID> liveClassIds,
      @Param("status") LiveClassRegistrantStatus status);
}
