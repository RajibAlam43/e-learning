package com.gii.common.repository.live;

import com.gii.common.entity.live.LiveClassSlot;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveClassSlotRepository extends JpaRepository<LiveClassSlot, UUID> {
  List<LiveClassSlot> findBySectionId(UUID sectionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT slot FROM LiveClassSlot slot WHERE slot.id = :id")
  Optional<LiveClassSlot> findByIdForUpdate(@Param("id") UUID id);

  @Query(
      """
        SELECT c.id, COUNT(slot)
        FROM Course c, LiveClassSlot slot
        WHERE c.id IN :courseIds
        AND slot.section.templateVersion.id = c.templateVersion.id
        AND slot.section.status = :sectionStatus
        AND slot.section.isMandatory = true
        AND slot.isMandatory = true
        AND NOT EXISTS (
          SELECT lc.id FROM LiveClass lc
          WHERE lc.course.id = c.id
          AND lc.slot.id = slot.id
          AND lc.status IN :excludedStatuses
        )
        GROUP BY c.id
      """)
  List<Object[]> countMandatoryByCourseIdsAndSectionStatus(
      @Param("courseIds") List<UUID> courseIds,
      @Param("sectionStatus") com.gii.common.enums.PublishStatus sectionStatus,
      @Param("excludedStatuses") List<com.gii.common.enums.LiveClassStatus> excludedStatuses);
}
