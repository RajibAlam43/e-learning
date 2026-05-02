package com.gii.api.studentapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StudentEnrollmentQueriesDataJpaTest extends AbstractStudentDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void enrollmentAndProgressQueriesReturnExpectedCounts() {
    var student = user("Jpa Student", "jpa-student@example.com");
    var creator = user("Jpa Creator", "jpa-creator@example.com");
    var course = course("Jpa Course", "jpa-course", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var l1 = lesson(course, sec, 1, PublishStatus.PUBLISHED, false);
    lesson(course, sec, 2, PublishStatus.PUBLISHED, false);
    enrollment(student, course, EnrollmentStatus.ACTIVE, null);
    completedProgress(student, l1);

    assertThat(enrollmentRepository.findByUserIdAndStatus(student.getId(), EnrollmentStatus.ACTIVE))
        .hasSize(1);
    assertThat(
            lessonProgressRepository.countCompletedByUserIdAndCourseIds(
                student.getId(), List.of(course.getId())))
        .hasSize(1);
    assertThat(
            lessonRepository.countByCourseIdsAndStatus(
                List.of(course.getId()), PublishStatus.PUBLISHED))
        .hasSize(1);
  }
}
