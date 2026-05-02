package com.gii.api.studentapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.PublishStatus;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StudentCertificatesQueriesDataJpaTest extends AbstractStudentDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void certificateQueriesReturnUserScopedRows() {
    var student = user("Cert Student", "cert-student@example.com");
    var other = user("Cert Other", "cert-other@example.com");
    var creator = user("Cert Creator", "cert-creator@example.com");
    var course = course("Cert Course", "cert-course", creator, PublishStatus.PUBLISHED);
    certificate(student, course, "C-STUDENT", false);
    certificate(other, course, "C-OTHER", false);

    assertThat(certificateRepository.findByUserIdOrderByIssuedAtDesc(student.getId())).hasSize(1);
    assertThat(
            certificateRepository.findActiveByUserIdAndCourseIds(
                student.getId(), List.of(course.getId())))
        .hasSize(1);
  }
}
