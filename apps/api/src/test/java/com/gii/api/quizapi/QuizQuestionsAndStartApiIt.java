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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
    quiz.setTitleEn("Quiz English");
    quizRepository.saveAndFlush(quiz);
    q1.setQuestionTextEn("Question One English");
    quizQuestionRepository.saveAndFlush(q1);
    var choiceA = choice(q1, "A", true);
    choiceA.setChoiceTextEn("Choice A English");
    quizChoiceRepository.saveAndFlush(choiceA);
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
            get("/learn/courses/{courseId}/quizzes/{quizId}", course.getId(), quiz.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quizId").value(quiz.getId().toString()))
        .andExpect(jsonPath("$.questions.length()").value(2))
        .andExpect(jsonPath("$.totalAttempts").value(1))
        .andExpect(jsonPath("$.remainingAttempts").value(2))
        .andExpect(jsonPath("$.bestScorePct").value(50))
        .andExpect(jsonPath("$.questions[0].choices[0].isCorrect").doesNotExist());

    mockMvc
        .perform(
            get("/learn/quizzes/{quizId}", quiz.getId())
                .param("lang", "en")
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quizTitle").value("Quiz English"))
        .andExpect(jsonPath("$.questions[0].questionText").value("Question One English"))
        .andExpect(jsonPath("$.questions[0].choices[0].choiceText").value("Choice A English"));
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

  @Test
  @Transactional
  void courseScopedAttemptsAreIsolatedAcrossRepeatedCoursesAndLegacyRouteRejectsAmbiguity()
      throws Exception {
    var creator = user("Creator", "creator-repeated-quiz@example.com");
    var student = user("Student", "student-repeated-quiz@example.com");
    var firstCourse =
        course("Repeated Quiz Course", "repeated-quiz-fall", creator, PublishStatus.PUBLISHED);
    var secondCourse = repeatedCourse(firstCourse, "repeated-quiz-spring", creator);
    var sec = section(firstCourse, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(firstCourse, sec, 1, PublishStatus.PUBLISHED);
    var firstEnrollment =
        enrollment(student, firstCourse, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    final var secondEnrollment =
        enrollment(student, secondCourse, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    var quiz = quiz(firstCourse, lesson, "Shared Quiz", PublishStatus.PUBLISHED, 60, 1, 600);
    question(quiz, 1, "Question", 10);
    var firstAttempt =
        attempt(
            quiz,
            student,
            firstEnrollment,
            1,
            100,
            true,
            Instant.now().minusSeconds(120),
            Instant.now().minusSeconds(60));

    mockMvc
        .perform(
            post(
                    "/learn/courses/{courseId}/quizzes/{quizId}/attempts",
                    secondCourse.getId(),
                    quiz.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attemptNumber").value(1));

    assertThat(
            quizAttemptRepository.countByQuizIdAndEnrollmentId(
                quiz.getId(), firstEnrollment.getId()))
        .isEqualTo(1);
    assertThat(
            quizAttemptRepository.countByQuizIdAndEnrollmentId(
                quiz.getId(), secondEnrollment.getId()))
        .isEqualTo(1);

    mockMvc
        .perform(
            get(
                    "/learn/courses/{courseId}/quizzes/{quizId}/attempts",
                    firstCourse.getId(),
                    quiz.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].attemptNumber").value(1));

    mockMvc
        .perform(
            get("/learn/quiz-attempts/{attemptId}", firstAttempt.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalAttempts").value(1))
        .andExpect(jsonPath("$.bestScorePct").value(100));

    mockMvc
        .perform(
            post("/learn/quizzes/{quizId}/attempts", quiz.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isConflict());
  }
}
