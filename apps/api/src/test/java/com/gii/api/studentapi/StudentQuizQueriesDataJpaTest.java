package com.gii.api.studentapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.PublishStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StudentQuizQueriesDataJpaTest extends AbstractStudentDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void findByCourseIdAndStatusOrderByPositionAscReturnsOnlyPublishedInOrder() {
    var creator = user("Quiz Jpa Creator", "quiz-jpa-creator@example.com");
    var course = course("Quiz Jpa Course", "quiz-jpa-course", creator, PublishStatus.PUBLISHED);
    var section = section(course, 1, PublishStatus.PUBLISHED);
    var second = quiz(course, section, 2, PublishStatus.PUBLISHED, "Second Quiz");
    var first = quiz(course, section, 1, PublishStatus.PUBLISHED, "First Quiz");
    quiz(course, section, 3, PublishStatus.DRAFT, "Draft Quiz");

    var result =
        quizRepository.findByCourseIdAndStatusOrderByPositionAsc(course.getId(), PublishStatus.PUBLISHED);

    assertThat(result).extracting(q -> q.getId()).containsExactly(first.getId(), second.getId());
    assertThat(result).allMatch(q -> q.getStatus() == PublishStatus.PUBLISHED);
  }
}
