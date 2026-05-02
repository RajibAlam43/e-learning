package com.gii.api.certificateapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.PublishStatus;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CertificateRepositoryDataJpaTest extends AbstractCertificateDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupCertificateData();
  }

  @Test
  void certificateQueriesReturnExpectedRows() {
    var creator = user("Creator", "creator-cert-jpa@example.com");
    var issuer = user("Issuer", "issuer-cert-jpa@example.com");
    var student = user("Student", "student-cert-jpa@example.com");
    var student2 = user("Student2", "student2-cert-jpa@example.com");
    var courseA = course("Course JPA A", "course-cert-jpa-a", creator, PublishStatus.PUBLISHED);
    var courseB = course("Course JPA B", "course-cert-jpa-b", creator, PublishStatus.PUBLISHED);
    final var cert1 =
        certificate(
            student, courseA, "GII-CERT-JPA11111", false, "https://cdn.test/jpa1.pdf", issuer);
    final var cert2 =
        certificate(
            student, courseB, "GII-CERT-JPA22222", true, "https://cdn.test/jpa2.pdf", issuer);
    certificate(student2, courseA, "GII-CERT-JPA33333", false, "https://cdn.test/jpa3.pdf", issuer);

    assertThat(certificateRepository.findByCertificateCode("GII-CERT-JPA11111")).isPresent();
    assertThat(certificateRepository.findByUserIdAndCourseId(student.getId(), courseA.getId()))
        .isPresent();
    assertThat(certificateRepository.findByIdAndUserId(cert1.getId(), student.getId())).isPresent();
    assertThat(certificateRepository.findByIdAndUserId(cert1.getId(), student2.getId())).isEmpty();
    assertThat(certificateRepository.findByUserIdOrderByIssuedAtDesc(student.getId())).hasSize(2);
    assertThat(
            certificateRepository.findActiveByUserIdAndCourseIds(
                student.getId(), List.of(courseA.getId(), courseB.getId())))
        .hasSize(1)
        .extracting("id")
        .contains(cert1.getId())
        .doesNotContain(cert2.getId());
    assertThat(certificateRepository.countByUserIdAndRevokedAtIsNull(student.getId())).isEqualTo(1);
  }
}
