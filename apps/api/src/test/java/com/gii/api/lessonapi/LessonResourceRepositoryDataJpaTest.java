package com.gii.api.lessonapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LessonResourceRepositoryDataJpaTest extends AbstractLessonDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupLessonData();
  }

  @Test
  void resourcesAreReturnedInPositionOrder() {
    var creator = user("Creator", "creator-jpa-resource@example.com");
    var course = course("Resource Course", "resource-course-jpa", creator, PublishStatus.PUBLISHED);
    var section = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(
            course, section, 1, PublishStatus.PUBLISHED, true, ReleaseType.IMMEDIATE, null, null);
    var r2 = resource(lesson, 2, "Second");
    var r1 = resource(lesson, 1, "First");

    var rows = lessonResourceRepository.findByLessonIdOrderByPositionAsc(lesson.getId());
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).getId()).isEqualTo(r1.getId());
    assertThat(rows.get(1).getId()).isEqualTo(r2.getId());
  }
}
