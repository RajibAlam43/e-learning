package com.gii.api.quizapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class QuizSubmitResultHistoryApiIT extends AbstractQuizApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupQuizData();
  }

  @Test
  void submitAttemptGradesAndPersistsAnswersThenResultAndHistoryExposeIt() throws Exception {
    var creator = user("Creator", "creator-quiz-submit@example.com");
    var student = user("Student", "student-quiz-submit@example.com");
    var course = course("Quiz Course 3", "quiz-course-3", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(7200));
    var quiz = quiz(course, lesson, "Quiz 3", PublishStatus.PUBLISHED, 60, 3, 900);
    var q1 = question(quiz, 1, "Q1", 5);
    var q2 = question(quiz, 2, "Q2", 5);
    var q1c1 = choice(q1, "Q1-A", true);
    var q1c2 = choice(q1, "Q1-B", false);
    var q2c1 = choice(q2, "Q2-A", false);
    var q2c2 = choice(q2, "Q2-B", true);
    var attempt = attempt(quiz, student, 1, null, null, Instant.now().minusSeconds(100), null);

    String submitBody =
        """
        {
          "answers": [
            {"questionId": "%s", "choiceId": "%s"},
            {"questionId": "%s", "choiceId": "%s"}
          ]
        }
        """
            .formatted(q1.getId(), q1c1.getId(), q2.getId(), q2c1.getId());

    mockMvc
        .perform(
            post("/learn/quiz-attempts/{attemptId}/submit", attempt.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(submitBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attemptId").value(attempt.getId().toString()))
        .andExpect(jsonPath("$.scorePct").value(50))
        .andExpect(jsonPath("$.passed").value(false))
        .andExpect(jsonPath("$.questionResults.length()").value(2));

    var saved = quizAttemptRepository.findById(attempt.getId()).orElseThrow();
    assertThat(saved.getSubmittedAt()).isNotNull();
    assertThat(saved.getScorePct()).isEqualTo(50);
    assertThat(quizAttemptAnswerRepository.findByAttemptId(attempt.getId())).hasSize(2);

    mockMvc
        .perform(
            get("/learn/quiz-attempts/{attemptId}", attempt.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.earnedPoints").value(5))
        .andExpect(jsonPath("$.totalPoints").value(10))
        .andExpect(jsonPath("$.nextAction").value("RETRY_QUIZ"));

    mockMvc
        .perform(
            get("/learn/quizzes/{quizId}/attempts", quiz.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].status").value("GRADED"));
  }

  @Test
  void submitAttemptRejectsWhenTimedOutAndWhenDuplicateQuestionProvided() throws Exception {
    var creator = user("Creator", "creator-quiz-submit2@example.com");
    var student = user("Student", "student-quiz-submit2@example.com");
    var course = course("Quiz Course 4", "quiz-course-4", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson1 = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    var lesson2 = lesson(course, sec, 2, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(7200));
    var quiz = quiz(course, lesson1, "Quiz 4", PublishStatus.PUBLISHED, 60, 3, 30);
    var q1 = question(quiz, 1, "Q1", 5);
    var c1 = choice(q1, "A", true);
    var c2 = choice(q1, "B", false);
    var expiredAttempt =
        attempt(quiz, student, 1, null, null, Instant.now().minusSeconds(100), null);

    String singleAnswer =
        """
        {"answers":[{"questionId":"%s","choiceId":"%s"}]}
        """
            .formatted(q1.getId(), c1.getId());
    mockMvc
        .perform(
            post("/learn/quiz-attempts/{attemptId}/submit", expiredAttempt.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(singleAnswer))
        .andExpect(status().isRequestTimeout());

    var quizNoTime = quiz(course, lesson2, "Quiz 5", PublishStatus.PUBLISHED, 60, 3, null);
    var qx = question(quizNoTime, 1, "QX", 5);
    var a = choice(qx, "A", true);
    var b = choice(qx, "B", false);
    var duplicateAttempt =
        attempt(quizNoTime, student, 1, null, null, Instant.now().minusSeconds(10), null);
    String duplicateAnswers =
        """
        {"answers":[
          {"questionId":"%s","choiceId":"%s"},
          {"questionId":"%s","choiceId":"%s"}
        ]}
        """
            .formatted(qx.getId(), a.getId(), qx.getId(), b.getId());
    mockMvc
        .perform(
            post("/learn/quiz-attempts/{attemptId}/submit", duplicateAttempt.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateAnswers))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submitAttemptRejectsWhenNotAllQuestionsAnswered() throws Exception {
    var creator = user("Creator", "creator-quiz-submit3@example.com");
    var student = user("Student", "student-quiz-submit3@example.com");
    var course = course("Quiz Course 5", "quiz-course-5", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(7200));
    var quiz = quiz(course, lesson, "Quiz 6", PublishStatus.PUBLISHED, 60, 3, null);
    var q1 = question(quiz, 1, "Q1", 5);
    question(quiz, 2, "Q2", 5);
    var c1 = choice(q1, "A", true);
    var attempt = attempt(quiz, student, 1, null, null, Instant.now().minusSeconds(10), null);

    String partialAnswer =
        """
        {"answers":[{"questionId":"%s","choiceId":"%s"}]}
        """
            .formatted(q1.getId(), c1.getId());

    mockMvc
        .perform(
            post("/learn/quiz-attempts/{attemptId}/submit", attempt.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(partialAnswer))
        .andExpect(status().isBadRequest());
  }
}
