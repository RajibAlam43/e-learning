package com.gii.api.quizapi;

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
import org.springframework.test.web.servlet.MockMvc;

class QuizQuestionsAndStartApiIt extends AbstractQuizApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupQuizData();
  }

  @Test
  void getQuizQuestionsReturnsQuestionsChoicesAndAttemptMeta() throws Exception {
    var creator = user("Creator", "creator-quiz-q1@example.com");
    var student = user("Student", "student-quiz-q1@example.com");
    var course = course("Quiz Course", "quiz-course", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    var quiz = quiz(course, lesson, "Quiz 1", PublishStatus.PUBLISHED, 60, 3, 600);
    var q1 = question(quiz, 1, "Q1", 5);
    var q2 = question(quiz, 2, "Q2", 5);
    choice(q1, "A", true);
    choice(q1, "B", false);
    choice(q2, "C", false);
    choice(q2, "D", true);
    attempt(
        quiz,
        student,
        1,
        50,
        false,
        Instant.now().minusSeconds(200),
        Instant.now().minusSeconds(150));

    mockMvc
        .perform(
            get("/learn/quizzes/{quizId}", quiz.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quizId").value(quiz.getId().toString()))
        .andExpect(jsonPath("$.questions.length()").value(2))
        .andExpect(jsonPath("$.totalAttempts").value(1))
        .andExpect(jsonPath("$.remainingAttempts").value(2))
        .andExpect(jsonPath("$.bestScorePct").value(50))
        .andExpect(jsonPath("$.questions[0].choices[0].isCorrect").doesNotExist());
  }

  @Test
  void startAttemptCreatesAttemptAndEnforcesMaxAttempts() throws Exception {
    var creator = user("Creator", "creator-quiz-start@example.com");
    var student = user("Student", "student-quiz-start@example.com");
    var course = course("Quiz Course 2", "quiz-course-2", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    var quiz = quiz(course, lesson, "Quiz 2", PublishStatus.PUBLISHED, 60, 1, 1200);
    question(quiz, 1, "Only question", 10);

    mockMvc
        .perform(
            post("/learn/quizzes/{quizId}/attempts", quiz.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attemptNumber").value(1))
        .andExpect(jsonPath("$.timeLimitSec").value(1200))
        .andExpect(jsonPath("$.totalQuestions").value(1));

    mockMvc
        .perform(
            post("/learn/quizzes/{quizId}/attempts", quiz.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isBadRequest());
  }
}
