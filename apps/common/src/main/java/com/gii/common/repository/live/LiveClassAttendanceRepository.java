package com.gii.common.repository.live;

import com.gii.common.entity.live.LiveClassAttendance;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveClassAttendanceRepository extends JpaRepository<LiveClassAttendance, UUID> {

  List<LiveClassAttendance> findByLiveClassId(UUID liveClassId);

  long countByUserId(UUID userId);
}
