package com.gii.api.quizapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.PublishStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QuizReadRepositoriesDataJpaTest extends AbstractQuizDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupQuizData();
  }

  @Test
  void quizQuestionAndChoiceQueriesReturnExpectedRows() {
    var creator = user("Creator", "creator-quiz-jpa1@example.com");
    var course = course("Quiz JPA 1", "quiz-jpa-1", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson1 = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    var lesson2 = lesson(course, sec, 2, PublishStatus.PUBLISHED);
    var publishedQuiz = quiz(course, lesson1, "Published", PublishStatus.PUBLISHED, 60, 3, 600);
    quiz(course, lesson2, "Draft", PublishStatus.DRAFT, 60, 3, 600);
    var q2 = question(publishedQuiz, 2, "Q2", 5);
    var q1 = question(publishedQuiz, 1, "Q1", 5);
    choice(q1, "Q1-A", true);
    choice(q1, "Q1-B", false);
    choice(q2, "Q2-A", false);

    assertThat(quizRepository.findByIdAndStatus(publishedQuiz.getId(), PublishStatus.PUBLISHED))
        .isPresent();
    assertThat(quizRepository.findByIdAndStatus(publishedQuiz.getId(), PublishStatus.DRAFT))
        .isEmpty();

    var orderedQuestions =
        quizQuestionRepository.findByQuizIdOrderByPositionAsc(publishedQuiz.getId());
    assertThat(orderedQuestions).hasSize(2);
    assertThat(orderedQuestions.get(0).getId()).isEqualTo(q1.getId());
    assertThat(orderedQuestions.get(1).getId()).isEqualTo(q2.getId());

    var allChoices =
        quizChoiceRepository.findByQuestionIdIn(
            orderedQuestions.stream().map(x -> x.getId()).toList());
    assertThat(allChoices).hasSize(3);
  }

  @Test
  void sectionQuizOrderingQueriesShouldReturnPositionOrder() {
    var creator = user("Creator", "creator-quiz-jpa2@example.com");
    var course = course("Quiz JPA 2", "quiz-jpa-2", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson1 = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    var lesson2 = lesson(course, sec, 2, PublishStatus.PUBLISHED);
    var quiz2 = quiz(course, lesson2, "Quiz 2", PublishStatus.PUBLISHED, 60, 3, 600);
    var quiz1 = quiz(course, lesson1, "Quiz 1", PublishStatus.PUBLISHED, 60, 3, 600);

    var quizzes = quizRepository.findBySectionIdOrderByPositionAsc(sec.getId());
    assertThat(quizzes).hasSize(2);
    assertThat(quizzes.get(0).getId()).isEqualTo(quiz1.getId());
    assertThat(quizzes.get(1).getId()).isEqualTo(quiz2.getId());
  }
}
