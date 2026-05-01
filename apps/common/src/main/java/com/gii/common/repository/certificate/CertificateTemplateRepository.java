package com.gii.common.repository.certificate;

import com.gii.common.entity.certificate.CertificateTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, UUID> {

  List<CertificateTemplate> findByIsActiveTrue();
}
