package com.gii.api.quizapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class QuizApiContractGapIt extends AbstractQuizApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupQuizData();
  }

  @Test
  void submitWithEmptyAnswersShouldReturn400() throws Exception {
    var creator = user("Creator", "creator-quiz-gap@example.com");
    var student = user("Student", "student-quiz-gap@example.com");
    var course = course("Quiz Course Gap", "quiz-course-gap", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(7200));
    var quiz = quiz(course, lesson, "Quiz Gap", PublishStatus.PUBLISHED, 60, 3, 300);
    var q1 = question(quiz, 1, "Q1", 5);
    var c1 = choice(q1, "A", true);
    var attempt = attempt(quiz, student, 1, null, null, Instant.now().minusSeconds(10), null);
    // keep one valid entity path so failure is about body validation only
    String body = "{\"answers\":[]}";

    mockMvc
        .perform(
            post("/learn/quiz-attempts/{attemptId}/submit", attempt.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }
}
