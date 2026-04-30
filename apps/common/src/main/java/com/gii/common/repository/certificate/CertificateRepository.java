package com.gii.common.repository.certificate;

import com.gii.common.entity.certificate.Certificate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

  Optional<Certificate> findByCertificateCode(String certificateCode);

  Optional<Certificate> findByUserIdAndCourseId(UUID userId, UUID courseId);

  List<Certificate> findByUserIdOrderByIssuedAtDesc(UUID userId);

  boolean existsByUserIdAndCourseIdAndRevokedAtIsNull(UUID userId, UUID courseId);

  long countByUserIdAndRevokedAtIsNull(UUID userId);
}
