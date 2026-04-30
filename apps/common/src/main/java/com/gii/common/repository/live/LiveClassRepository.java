package com.gii.common.repository.live;

import com.gii.common.entity.live.LiveClass;
import com.gii.common.enums.LiveClassStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LiveClassRepository extends JpaRepository<LiveClass, UUID> {

  Optional<LiveClass> findById(UUID id);

  @Query(
      """
        SELECT lc FROM LiveClass lc
        WHERE lc.course.id IN :courseIds
        AND lc.status IN :statuses
        AND lc.endsAt >= :now
        ORDER BY lc.startsAt ASC
      """)
  List<LiveClass> findUpcomingByCourseIds(
      List<UUID> courseIds, List<LiveClassStatus> statuses, Instant now);

  @Query(
      """
        SELECT lc FROM LiveClass lc
        WHERE lc.course.id = :courseId
        ORDER BY lc.startsAt ASC
      """)
  List<LiveClass> findByCourseIdOrderByStartsAtAsc(UUID courseId);

  List<LiveClass> findByInstructorIdOrderByStartsAtAsc(UUID instructorId);
}
