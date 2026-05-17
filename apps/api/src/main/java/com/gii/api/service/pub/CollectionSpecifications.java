package com.gii.api.service.pub;

import com.gii.common.entity.collection.Collection;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.PublishStatus;
import org.springframework.data.jpa.domain.Specification;

public final class CollectionSpecifications {

  private CollectionSpecifications() {}

  public static Specification<Collection> hasStatus(PublishStatus status) {
    return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
  }

  public static Specification<Collection> hasType(CollectionType type) {
    return (root, query, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
  }
}
