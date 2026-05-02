package com.gii.common.repository.course;

import com.gii.common.entity.course.SectionItem;
import com.gii.common.enums.SectionItemType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionItemRepository extends JpaRepository<SectionItem, UUID> {

  List<SectionItem> findBySectionIdOrderByPositionAsc(UUID sectionId);

  Optional<SectionItem> findByItemTypeAndItemId(SectionItemType itemType, UUID itemId);

  Optional<SectionItem> findBySectionIdAndPosition(UUID sectionId, Integer position);

  boolean existsBySectionIdAndPosition(UUID sectionId, Integer position);

  void deleteByItemTypeAndItemId(SectionItemType itemType, UUID itemId);
}
