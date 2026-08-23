package com.gii.common.repository.live;

import com.gii.common.entity.live.LiveClass;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveClassRepository extends JpaRepository<LiveClass, UUID> {

  @Query(
      value =
          """
            SELECT lc FROM LiveClass lc
            JOIN FETCH lc.course
            LEFT JOIN FETCH lc.instructor
          """,
      countQuery = "SELECT COUNT(lc) FROM LiveClass lc")
  Page<LiveClass> findAdminPage(Pageable pageable);

  @Query(
      value =
          """
            SELECT lc FROM LiveClass lc
            JOIN FETCH lc.course
            LEFT JOIN FETCH lc.instructor
            WHERE lc.status IN :statuses
          """,
      countQuery = "SELECT COUNT(lc) FROM LiveClass lc WHERE lc.status IN :statuses")
  Page<LiveClass> findAdminPageByStatuses(
      @Param("statuses") List<LiveClassStatus> statuses, Pageable pageable);

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

  List<LiveClass> findBySectionIdOrderByStartsAtAsc(UUID sectionId);

  @Query(
      """
        SELECT lc.course.id, COUNT(lc)
        FROM LiveClass lc
        WHERE lc.course.id IN :courseIds
        GROUP BY lc.course.id
      """)
  List<Object[]> countByCourseIds(@Param("courseIds") List<UUID> courseIds);

  @Query(
      """
        SELECT lc.course.id, COUNT(lc)
        FROM LiveClass lc
        WHERE lc.course.id IN :courseIds
        AND lc.section.status = :sectionStatus
        AND lc.status IN :statuses
        GROUP BY lc.course.id
      """)
  List<Object[]> countCompletableByCourseIdsAndStatuses(
      @Param("courseIds") List<UUID> courseIds,
      @Param("sectionStatus") PublishStatus sectionStatus,
      @Param("statuses") List<LiveClassStatus> statuses);

  @Query(
      """
        SELECT lc.course.id, COUNT(lc)
        FROM LiveClass lc
        WHERE lc.course.id IN :courseIds
        AND lc.section.status = :sectionStatus
        AND lc.status = :liveClassStatus
        GROUP BY lc.course.id
      """)
  List<Object[]> countByCourseIdsAndSectionStatusAndLiveClassStatus(
      @Param("courseIds") List<UUID> courseIds,
      @Param("sectionStatus") PublishStatus sectionStatus,
      @Param("liveClassStatus") LiveClassStatus liveClassStatus);

  @Query(
      """
        SELECT COUNT(lc) > 0
        FROM LiveClass lc
        WHERE lc.provider = :provider
        AND lc.status IN :statuses
        AND lc.startsAt < :endsAt
        AND lc.endsAt > :startsAt
      """)
  boolean existsOverlappingByProvider(
      @Param("provider") LiveClassProvider provider,
      @Param("statuses") List<LiveClassStatus> statuses,
      @Param("startsAt") Instant startsAt,
      @Param("endsAt") Instant endsAt);
}
