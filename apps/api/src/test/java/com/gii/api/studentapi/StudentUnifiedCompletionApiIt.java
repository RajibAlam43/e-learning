package com.gii.api.studentapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.entity.quiz.QuizAttempt;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class StudentUnifiedCompletionApiIt extends AbstractStudentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void allProgressViewsCountPublishedLessonsAndPassedQuizzesConsistently() throws Exception {
    var creator = user("Creator", "completion-creator@example.com");
    var student = user("Student", "completion-student@example.com");
    var course = course("Completion Course", "completion-course", creator, PublishStatus.PUBLISHED);
    var section = section(course, 1, PublishStatus.PUBLISHED);
    var completedLesson = lesson(course, section, 1, PublishStatus.PUBLISHED, false);
    final var pendingLesson = lesson(course, section, 2, PublishStatus.PUBLISHED, false);
    var passedQuiz = quiz(course, section, 3, PublishStatus.PUBLISHED, "Passed Quiz");
    final var failedQuiz = quiz(course, section, 4, PublishStatus.PUBLISHED, "Failed Quiz");
    enrollment(student, course, EnrollmentStatus.ACTIVE, null);
    completedProgress(student, completedLesson);
    Instant attemptStartedAt = Instant.now().minusSeconds(1);
    quizAttemptRepository.save(
        QuizAttempt.builder()
            .quiz(passedQuiz)
            .user(student)
            .attemptNo(1)
            .scorePct(90)
            .passed(true)
            .startedAt(attemptStartedAt)
            .submittedAt(Instant.now())
            .build());
    quizAttemptRepository.save(
        QuizAttempt.builder()
            .quiz(failedQuiz)
            .user(student)
            .attemptNo(1)
            .scorePct(40)
            .passed(false)
            .startedAt(attemptStartedAt)
            .submittedAt(Instant.now())
            .build());

    mockMvc
        .perform(get("/student/courses").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].completedLessons").value(1))
        .andExpect(jsonPath("$[0].totalLessons").value(2))
        .andExpect(jsonPath("$[0].completedItems").value(2))
        .andExpect(jsonPath("$[0].totalItems").value(4))
        .andExpect(jsonPath("$[0].completionPercentage").value(50.0));

    mockMvc
        .perform(
            get("/student/courses/{courseId}", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completedItems").value(2))
        .andExpect(jsonPath("$.totalItems").value(4))
        .andExpect(jsonPath("$.sections[0].completedItems").value(2))
        .andExpect(jsonPath("$.sections[0].totalItems").value(4))
        .andExpect(jsonPath("$.sections[0].quizzes[0].completed").value(true))
        .andExpect(jsonPath("$.sections[0].quizzes[1].completed").value(false));

    mockMvc
        .perform(
            get("/learn/courses/{courseId}/progress", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completedItems").value(2))
        .andExpect(jsonPath("$.totalItems").value(4))
        .andExpect(jsonPath("$.pendingItems").value(2))
        .andExpect(jsonPath("$.completionPercentage").value(50.0));

    mockMvc
        .perform(
            post("/student/courses/{courseId}/certificate", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isForbidden());

    completedProgress(student, pendingLesson);
    quizAttemptRepository.save(
        QuizAttempt.builder()
            .quiz(failedQuiz)
            .user(student)
            .attemptNo(2)
            .scorePct(80)
            .passed(true)
            .startedAt(Instant.now().minusSeconds(1))
            .submittedAt(Instant.now())
            .build());

    mockMvc
        .perform(
            post("/student/courses/{courseId}/certificate", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.wasEligible").value(true));
  }

  @Test
  void collectionProgressIncludesPassedQuizItems() throws Exception {
    var creator = user("Creator", "collection-completion-creator@example.com");
    var student = user("Student", "collection-completion-student@example.com");
    var course =
        course(
            "Collection Course", "collection-completion-course", creator, PublishStatus.PUBLISHED);
    var section = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, section, 1, PublishStatus.PUBLISHED, false);
    final var quiz = quiz(course, section, 2, PublishStatus.PUBLISHED, "Collection Quiz");
    var collection =
        collection("Completion Pack", "completion-pack", creator, PublishStatus.PUBLISHED);
    collectionCourse(collection, course, 1, true);
    collectionEnrollment(student, collection, EnrollmentStatus.ACTIVE, null);
    completedProgress(student, lesson);
    quizAttemptRepository.save(
        QuizAttempt.builder()
            .quiz(quiz)
            .user(student)
            .attemptNo(1)
            .scorePct(85)
            .passed(true)
            .startedAt(Instant.now().minusSeconds(1))
            .submittedAt(Instant.now())
            .build());

    mockMvc
        .perform(get("/student/collections").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].completedLessons").value(1))
        .andExpect(jsonPath("$[0].totalLessons").value(1))
        .andExpect(jsonPath("$[0].completedItems").value(2))
        .andExpect(jsonPath("$[0].totalItems").value(2))
        .andExpect(jsonPath("$[0].progressPercentage").value(100.0));

    mockMvc
        .perform(
            get("/student/collections/{collectionId}", collection.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completedItems").value(2))
        .andExpect(jsonPath("$.totalItems").value(2))
        .andExpect(jsonPath("$.courses[0].completedItems").value(2));
  }

  @Test
  void draftSectionsAndDraftItemsDoNotContributeToCompletion() throws Exception {
    var creator = user("Creator", "completion-draft-creator@example.com");
    final var student = user("Student", "completion-draft-student@example.com");
    var course = course("Draft Items", "draft-items", creator, PublishStatus.PUBLISHED);
    var publishedSection = section(course, 1, PublishStatus.PUBLISHED);
    var draftSection = section(course, 2, PublishStatus.DRAFT);
    final var visibleLesson = lesson(course, publishedSection, 1, PublishStatus.PUBLISHED, false);
    lesson(course, publishedSection, 2, PublishStatus.DRAFT, false);
    lesson(course, draftSection, 1, PublishStatus.PUBLISHED, false);
    quiz(course, draftSection, 2, PublishStatus.PUBLISHED, "Hidden Quiz");
    enrollment(student, course, EnrollmentStatus.ACTIVE, null);
    completedProgress(student, visibleLesson);

    mockMvc
        .perform(
            get("/learn/courses/{courseId}/progress", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completedItems").value(1))
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.completionPercentage").value(100.0));
  }
}
