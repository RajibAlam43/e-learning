package com.gii.api.lessonapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LessonProgressRepositoryDataJpaTest extends AbstractLessonDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupLessonData();
  }

  @Test
  void progressQueriesReturnExpectedCountsAndLatestActivity() {
    var creator = user("Creator", "creator-jpa-progress@example.com");
    var student = user("Student", "student-jpa-progress@example.com");
    var courseA = course("Course A", "course-a-jpa", creator, PublishStatus.PUBLISHED);
    var sectionA = section(courseA, 1, PublishStatus.PUBLISHED);
    var lessonA1 =
        lesson(
            courseA,
            sectionA,
            1,
            PublishStatus.PUBLISHED,
            false,
            ReleaseType.IMMEDIATE,
            null,
            null);
    var lessonA2 =
        lesson(
            courseA,
            sectionA,
            2,
            PublishStatus.PUBLISHED,
            false,
            ReleaseType.IMMEDIATE,
            null,
            null);
    enrollment(student, courseA, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    progress(student, lessonA1, true, 100);
    progress(student, lessonA2, false, 25);

    assertThat(
            lessonProgressRepository.findByUserIdAndLessonCourseId(
                student.getId(), courseA.getId()))
        .hasSize(2);
    assertThat(
            lessonProgressRepository.countByUserIdAndLessonCourseIdAndCompletedAtIsNotNull(
                student.getId(), courseA.getId()))
        .isEqualTo(1);
    assertThat(lessonProgressRepository.findLatestActivityAtByUserId(student.getId())).isNotNull();
  }
}
