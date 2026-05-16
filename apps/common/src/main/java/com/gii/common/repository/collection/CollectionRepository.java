package com.gii.common.repository.collection;

import com.gii.common.entity.collection.Collection;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CollectionRepository
    extends JpaRepository<Collection, UUID>, JpaSpecificationExecutor<Collection> {

  Optional<Collection> findBySlugAndStatus(String slug, PublishStatus status);

  List<Collection> findByIdInAndStatus(List<UUID> ids, PublishStatus status);
}
