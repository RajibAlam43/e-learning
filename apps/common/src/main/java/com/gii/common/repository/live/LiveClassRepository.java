package com.gii.common.repository.live;

import com.gii.common.entity.live.LiveClass;
import com.gii.common.enums.LiveClassStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveClassRepository extends JpaRepository<LiveClass, UUID> {

  Optional<LiveClass> findById(UUID id);

  Optional<LiveClass> findByIdAndInstructorId(UUID id, UUID instructorId);

  @Query(
      """
        SELECT lc FROM LiveClass lc
        WHERE lc.course.id IN :courseIds
        AND lc.status IN :statuses
        AND lc.endsAt >= :now
        ORDER BY lc.startsAt ASC
      """)
  List<LiveClass> findUpcomingByCourseIds(
      @Param("courseIds") List<UUID> courseIds,
      @Param("statuses") List<LiveClassStatus> statuses,
      @Param("now") Instant now);

  @Query(
      """
        SELECT lc FROM LiveClass lc
        WHERE lc.course.id = :courseId
        ORDER BY lc.startsAt ASC
      """)
  List<LiveClass> findByCourseIdOrderByStartsAtAsc(@Param("courseId") UUID courseId);

  @Query(
      """
        SELECT lc.course.id, COUNT(lc)
        FROM LiveClass lc
        WHERE lc.course.id IN :courseIds
        GROUP BY lc.course.id
      """)
  List<Object[]> countByCourseIds(@Param("courseIds") List<UUID> courseIds);
}
