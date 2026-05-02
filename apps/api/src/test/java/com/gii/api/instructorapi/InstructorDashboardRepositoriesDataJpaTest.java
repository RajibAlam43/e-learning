package com.gii.api.instructorapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InstructorDashboardRepositoriesDataJpaTest extends AbstractInstructorDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupInstructorData();
  }

  @Test
  void aggregateCountQueriesReturnExpectedValues() {
    var creator = user("Creator", "creator-inst-jpa2@example.com");
    var instructor = user("Instructor", "inst-jpa2@example.com");
    final var studentA = user("Student A", "student-a-inst-jpa2@example.com");
    final var studentB = user("Student B", "student-b-inst-jpa2@example.com");
    var course = course("Course JPA 2", "course-jpa-inst-2", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec1 = section(course, 1, PublishStatus.PUBLISHED);
    var sec2 = section(course, 2, PublishStatus.PUBLISHED);
    lesson(course, sec1, 1, PublishStatus.PUBLISHED);
    lesson(course, sec2, 1, PublishStatus.PUBLISHED);
    enrollment(studentA, course, EnrollmentStatus.ACTIVE, null);
    enrollment(studentB, course, EnrollmentStatus.ACTIVE, null)
        .setCompletedAt(Instant.now().minusSeconds(50));
    enrollmentRepository.flush();

    assertThat(courseInstructorRepository.findByInstructorId(instructor.getId())).hasSize(1);
    assertThat(
            enrollmentRepository.countByCourseIdsAndStatus(
                List.of(course.getId()), EnrollmentStatus.ACTIVE))
        .hasSize(1);
    assertThat(
            enrollmentRepository.countCompletedByCourseIdsAndStatus(
                List.of(course.getId()), EnrollmentStatus.ACTIVE))
        .hasSize(1);
    assertThat(courseSectionRepository.countByCourseIds(List.of(course.getId()))).hasSize(1);
    assertThat(
            lessonRepository.countByCourseIdsAndStatus(
                List.of(course.getId()), PublishStatus.PUBLISHED))
        .hasSize(1);
  }
}
