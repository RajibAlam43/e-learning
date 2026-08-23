package com.gii.common.repository.live;

import com.gii.common.entity.live.LiveClassAttendance;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveClassAttendanceRepository extends JpaRepository<LiveClassAttendance, UUID> {

  List<LiveClassAttendance> findByLiveClassId(UUID liveClassId);

  @Query(
      """
        SELECT COUNT(DISTINCT a.liveClass.id)
        FROM LiveClassAttendance a
        WHERE a.user.id = :userId
        AND a.joinedAt IS NOT NULL
      """)
  long countDistinctAttendedLiveClassesByUserId(@Param("userId") UUID userId);

  @Query(
      """
        SELECT DISTINCT a.liveClass.id
        FROM LiveClassAttendance a
        WHERE a.user.id = :userId
        AND a.liveClass.course.id = :courseId
        AND a.liveClass.section.status = :sectionStatus
        AND a.liveClass.status IN :statuses
        AND a.joinedAt IS NOT NULL
      """)
  List<UUID> findAttendedCompletableLiveClassIds(
      @Param("userId") UUID userId,
      @Param("courseId") UUID courseId,
      @Param("sectionStatus") PublishStatus sectionStatus,
      @Param("statuses") List<LiveClassStatus> statuses);
}
