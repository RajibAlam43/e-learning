package com.gii.api.certificateapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.PublishStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CourseInstructorRepositoryDataJpaTest extends AbstractCertificateDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupCertificateData();
  }

  @Test
  void findByCourseIdReturnsInstructorRowsForCertificateServices() {
    var creator = user("Creator", "creator-cert-jpa2@example.com");
    var instructor = user("Instructor", "instructor-cert-jpa2@example.com");
    var course = course("Course Inst", "course-inst-jpa", creator, PublishStatus.PUBLISHED);
    primaryInstructor(course, instructor);

    var rows = courseInstructorRepository.findByCourseId(course.getId());
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getInstructor().getId()).isEqualTo(instructor.getId());
  }
}
