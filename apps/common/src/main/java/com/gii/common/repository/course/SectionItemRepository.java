package com.gii.common.repository.course;

import com.gii.common.entity.course.SectionItem;
import com.gii.common.enums.SectionItemType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SectionItemRepository extends JpaRepository<SectionItem, UUID> {

  List<SectionItem> findBySectionIdOrderByPositionAsc(UUID sectionId);

  List<SectionItem> findBySectionIdInOrderBySectionIdAscPositionAsc(List<UUID> sectionIds);

  Optional<SectionItem> findByItemTypeAndItemId(SectionItemType itemType, UUID itemId);

  Optional<SectionItem> findBySectionIdAndPosition(UUID sectionId, Integer position);

  boolean existsBySectionIdAndPosition(UUID sectionId, Integer position);

  @Query(
      "SELECT COALESCE(MAX(si.position), 0) FROM SectionItem si WHERE si.section.id = :sectionId")
  int findMaxPositionBySectionId(@Param("sectionId") UUID sectionId);

  void deleteByItemTypeAndItemId(SectionItemType itemType, UUID itemId);
}
