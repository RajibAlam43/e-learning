package com.gii.api.lessonapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.MediaStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class LessonContentApiIt extends AbstractLessonApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupLessonData();
  }

  @Test
  void getLessonContentReturnsLessonMediaResourcesAndProgress() throws Exception {
    var creator = user("Creator", "creator-lesson-content@example.com");
    var student = user("Student", "student-lesson-content@example.com");
    var course = course("Course One", "course-one", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    String thumbnailKey = "courses/" + course.getId() + "/thumbnails/course-content.webp";
    course.setThumbnailObjectKey(thumbnailKey);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    course.setTitleEn("Course One English");
    courseRepository.saveAndFlush(course);
    lesson.setTitleEn("Lesson English");
    lessonRepository.saveAndFlush(lesson);
    var media = mediaAsset(lesson, MediaProvider.BUNNY, MediaStatus.READY);
    media.setTitleEn("Media English");
    mediaAssetRepository.saveAndFlush(media);
    var slides = resource(lesson, 2, "Slides");
    slides.setTitleEn("Slides English");
    lessonResourceRepository.saveAndFlush(slides);
    var worksheet = resource(lesson, 1, "Worksheet");
    worksheet.setTitleEn("Worksheet English");
    lessonResourceRepository.saveAndFlush(worksheet);
    progress(student, lesson, false, 84);

    mockMvc
        .perform(
            get("/learn/lessons/{lessonId}", lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lessonId").value(lesson.getId().toString()))
        .andExpect(jsonPath("$.courseId").value(course.getId().toString()))
        .andExpect(jsonPath("$.userProgress.lastPositionSec").value(84))
        .andExpect(jsonPath("$.resources[0].title").value("Worksheet"))
        .andExpect(jsonPath("$.resources[1].title").value("Slides"))
        .andExpect(jsonPath("$.mediaPlayback.provider").value("BUNNY"))
        .andExpect(jsonPath("$.thumbnailUrl").doesNotExist())
        .andExpect(
            jsonPath("$.mediaPlayback.thumbnailUrl").value("https://assets.test/" + thumbnailKey));

    mockMvc
        .perform(
            get("/learn/lessons/{lessonId}", lesson.getId())
                .param("lang", "en")
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Lesson English"))
        .andExpect(jsonPath("$.courseName").value("Course One English"))
        .andExpect(jsonPath("$.resources[0].title").value("Worksheet English"))
        .andExpect(jsonPath("$.resources[1].title").value("Slides English"))
        .andExpect(jsonPath("$.mediaPlayback.title").value("Media English"));
  }

  @Test
  void getLessonContentRejectsWhenNotEnrolled() throws Exception {
    var creator = user("Creator", "creator-lesson-content2@example.com");
    var student = user("Student", "student-lesson-content2@example.com");
    var course = course("Course Two", "course-two", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);

    mockMvc
        .perform(
            get("/learn/lessons/{lessonId}", lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isForbidden());
  }

  @Test
  void getLessonContentRejectsWhenReleaseDateInFuture() throws Exception {
    var creator = user("Creator", "creator-lesson-content3@example.com");
    var student = user("Student", "student-lesson-content3@example.com");
    var course = course("Course Three", "course-three", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(
            course,
            sec,
            1,
            PublishStatus.PUBLISHED,
            false,
            ReleaseType.FIXED_DATE,
            Instant.now().plusSeconds(86400),
            null);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));

    mockMvc
        .perform(
            get("/learn/lessons/{lessonId}", lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isForbidden());
  }
}
