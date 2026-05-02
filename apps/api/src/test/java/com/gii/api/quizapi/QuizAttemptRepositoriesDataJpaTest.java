package com.gii.api.quizapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QuizAttemptRepositoriesDataJpaTest extends AbstractQuizDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupQuizData();
  }

  @Test
  void attemptAndAttemptAnswerQueriesSupportQuizFlows() {
    var creator = user("Creator", "creator-quiz-jpa2@example.com");
    var student = user("Student", "student-quiz-jpa2@example.com");
    var course = course("Quiz JPA 2", "quiz-jpa-2", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    var quiz = quiz(course, lesson, "Q", PublishStatus.PUBLISHED, 60, 3, 600);
    var q1 = question(quiz, 1, "Q1", 5);
    var q2 = question(quiz, 2, "Q2", 5);
    var c1 = choice(q1, "A", true);
    var c2 = choice(q2, "B", true);
    var a1 =
        attempt(
            quiz,
            student,
            1,
            50,
            false,
            Instant.now().minusSeconds(100),
            Instant.now().minusSeconds(60));
    final var a2 = attempt(quiz, student, 2, null, null, Instant.now().minusSeconds(20), null);
    attemptAnswer(a1, q1, c1);
    attemptAnswer(a1, q2, c2);

    assertThat(quizAttemptRepository.countByQuizIdAndUserId(quiz.getId(), student.getId()))
        .isEqualTo(2);
    var attempts =
        quizAttemptRepository.findByQuizIdAndUserIdOrderByAttemptNoDesc(
            quiz.getId(), student.getId());
    assertThat(attempts).hasSize(2);
    assertThat(attempts.get(0).getAttemptNo()).isEqualTo(2);
    assertThat(quizAttemptRepository.findByIdAndUserId(a2.getId(), student.getId())).isPresent();

    assertThat(quizAttemptAnswerRepository.findByAttemptId(a1.getId())).hasSize(2);
    quizAttemptAnswerRepository.deleteByAttemptId(a1.getId());
    assertThat(quizAttemptAnswerRepository.findByAttemptId(a1.getId())).isEmpty();
  }
}
