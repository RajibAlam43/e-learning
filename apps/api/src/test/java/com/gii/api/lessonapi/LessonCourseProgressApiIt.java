package com.gii.api.lessonapi;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class LessonCourseProgressApiIt extends AbstractLessonApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupLessonData();
  }

  @Test
  void courseProgressReturnsOnlyCourseIdAndCompletionPercentage() throws Exception {
    var creator = user("Creator", "creator-cp@example.com");
    var student = user("Student", "student-cp@example.com");
    var course = course("Course Progress", "course-progress", creator, PublishStatus.PUBLISHED);
    var sectionOne = section(course, 1, PublishStatus.PUBLISHED);
    var sectionTwo = section(course, 2, PublishStatus.PUBLISHED);
    var lessonOne =
        lesson(
            course,
            sectionOne,
            1,
            PublishStatus.PUBLISHED,
            false,
            ReleaseType.IMMEDIATE,
            null,
            null);
    final var lessonTwo =
        lesson(
            course,
            sectionOne,
            2,
            PublishStatus.PUBLISHED,
            false,
            ReleaseType.IMMEDIATE,
            null,
            null);
    lesson(
        course, sectionTwo, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    progress(student, lessonOne, true, 100);
    progress(student, lessonTwo, false, 45);

    mockMvc
        .perform(
            get("/learn/courses/{courseId}/progress", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.courseId").value(course.getId().toString()))
        .andExpect(jsonPath("$.completionPercentage").value(33.33))
        .andExpect(jsonPath("$").value(aMapWithSize(2)));
  }
}
