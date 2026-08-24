package com.gii.api.lessonapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.entity.enrollment.LessonProgressId;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

class LessonProgressApiIt extends AbstractLessonApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupLessonData();
  }

  @Test
  void saveProgressCreatesOrUpdatesProgressRow() throws Exception {
    var creator = user("Creator", "creator-progress@example.com");
    var student = user("Student", "student-progress@example.com");
    var course = course("Progress Course", "progress-course", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    var enrollment =
        enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post("/learn/lessons/{lessonId}/progress", lesson.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":false,\"lastPositionSec\":95}"))
        .andExpect(status().isOk());

    var saved =
        lessonProgressRepository
            .findById(
                LessonProgressId.builder()
                    .enrollmentId(enrollment.getId())
                    .lessonId(lesson.getId())
                    .build())
            .orElseThrow();
    assertThat(saved.getLastPositionSec()).isEqualTo(95);
    assertThat(saved.getCompletedAt()).isNull();

    mockMvc
        .perform(
            post("/learn/lessons/{lessonId}/progress", lesson.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":true,\"lastPositionSec\":120}"))
        .andExpect(status().isOk());

    assertThat(studentLearningStreakRepository.findById(student.getId()))
        .get()
        .extracting("currentStreak", "maxStreak")
        .containsExactly(1, 1);
  }

  @Test
  void markCompleteSetsCompletedAt() throws Exception {
    var creator = user("Creator", "creator-complete@example.com");
    var student = user("Student", "student-complete@example.com");
    var course = course("Complete Course", "complete-course", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    var enrollment =
        enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post("/learn/lessons/{lessonId}/complete", lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk());

    var saved =
        lessonProgressRepository
            .findById(
                LessonProgressId.builder()
                    .enrollmentId(enrollment.getId())
                    .lessonId(lesson.getId())
                    .build())
            .orElseThrow();
    assertThat(saved.getCompletedAt()).isNotNull();
    assertThat(enrollmentRepository.findById(enrollment.getId()).orElseThrow().getCompletedAt())
        .isNotNull();
    assertThat(studentLearningStreakRepository.findById(student.getId()))
        .get()
        .extracting("currentStreak", "maxStreak")
        .containsExactly(1, 1);
  }

  @Test
  @Transactional
  void courseScopedProgressIsIsolatedAcrossRepeatedCoursesAndLegacyRouteRejectsAmbiguity()
      throws Exception {
    var creator = user("Creator", "creator-repeated-progress@example.com");
    var student = user("Student", "student-repeated-progress@example.com");
    var firstCourse =
        course("Repeated Course", "repeated-course-fall", creator, PublishStatus.PUBLISHED);
    var secondCourse = repeatedCourse(firstCourse, "repeated-course-spring", creator);
    var sec = section(firstCourse, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(
            firstCourse, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    var firstEnrollment =
        enrollment(student, firstCourse, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    var secondEnrollment =
        enrollment(student, secondCourse, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            post(
                    "/learn/courses/{courseId}/lessons/{lessonId}/complete",
                    firstCourse.getId(),
                    lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk());

    assertThat(
            lessonProgressRepository.findById(
                LessonProgressId.builder()
                    .enrollmentId(firstEnrollment.getId())
                    .lessonId(lesson.getId())
                    .build()))
        .isPresent();
    assertThat(
            lessonProgressRepository.findById(
                LessonProgressId.builder()
                    .enrollmentId(secondEnrollment.getId())
                    .lessonId(lesson.getId())
                    .build()))
        .isEmpty();

    mockMvc
        .perform(
            post(
                    "/learn/courses/{courseId}/lessons/{lessonId}/progress",
                    secondCourse.getId(),
                    lesson.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":false,\"lastPositionSec\":55}"))
        .andExpect(status().isOk());

    assertThat(
            lessonProgressRepository
                .findById(
                    LessonProgressId.builder()
                        .enrollmentId(secondEnrollment.getId())
                        .lessonId(lesson.getId())
                        .build())
                .orElseThrow()
                .getLastPositionSec())
        .isEqualTo(55);

    mockMvc
        .perform(
            post("/learn/lessons/{lessonId}/complete", lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isConflict());
  }
}
