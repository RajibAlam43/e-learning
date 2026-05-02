package com.gii.common.repository.certificate;

import com.gii.common.entity.certificate.Certificate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

  Optional<Certificate> findByCertificateCode(String certificateCode);

  Optional<Certificate> findByUserIdAndCourseId(UUID userId, UUID courseId);

  Optional<Certificate> findByIdAndUserId(UUID id, UUID userId);

  List<Certificate> findByUserIdOrderByIssuedAtDesc(UUID userId);

  @Query(
      """
        SELECT c
        FROM Certificate c
        WHERE c.user.id = :userId
        AND c.course.id IN :courseIds
        AND c.revokedAt IS NULL
      """)
  List<Certificate> findActiveByUserIdAndCourseIds(
      @Param("userId") UUID userId, @Param("courseIds") List<UUID> courseIds);

  long countByUserIdAndRevokedAtIsNull(UUID userId);
}
