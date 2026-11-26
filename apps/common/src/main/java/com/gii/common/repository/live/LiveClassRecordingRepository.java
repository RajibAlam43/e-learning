package com.gii.common.repository.live;

import com.gii.common.entity.live.LiveClassRecording;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveClassRecordingRepository extends JpaRepository<LiveClassRecording, UUID> {
  List<LiveClassRecording> findByLiveClassIdOrderByCreatedAtDesc(UUID liveClassId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM LiveClassRecording r JOIN FETCH r.liveClass WHERE r.id = :id")
  Optional<LiveClassRecording> findLocked(@Param("id") UUID id);

  @Query(
      value =
          """
          SELECT * FROM live_class_recordings
          WHERE status = 'QUEUED' OR (status = 'PROCESSING' AND processing_until < CURRENT_TIMESTAMP)
          ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED
          """,
      nativeQuery = true)
  Optional<LiveClassRecording> claimNext();
}
