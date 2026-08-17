package com.gii.common.repository.collection;

import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.enums.EnrollmentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionEnrollmentRepository extends JpaRepository<CollectionEnrollment, UUID> {

  Optional<CollectionEnrollment> findByUserIdAndCollectionId(UUID userId, UUID collectionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
        SELECT ce
        FROM CollectionEnrollment ce
        WHERE ce.user.id = :userId
        AND ce.collection.id = :collectionId
      """)
  Optional<CollectionEnrollment> findByUserIdAndCollectionIdForUpdate(
      @Param("userId") UUID userId, @Param("collectionId") UUID collectionId);

  Optional<CollectionEnrollment> findByUserIdAndCollectionIdAndStatus(
      UUID userId, UUID collectionId, EnrollmentStatus status);

  boolean existsByUserIdAndCollectionIdAndStatus(
      UUID userId, UUID collectionId, EnrollmentStatus status);

  List<CollectionEnrollment> findByUserIdAndStatus(UUID userId, EnrollmentStatus status);
}
