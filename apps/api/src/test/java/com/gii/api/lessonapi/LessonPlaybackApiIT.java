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

class LessonPlaybackApiIT extends AbstractLessonApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupLessonData();
  }

  @Test
  void getPlaybackReturnsProviderUrlsForEligibleStudent() throws Exception {
    var creator = user("Creator", "creator-playback@example.com");
    var student = user("Student", "student-playback@example.com");
    var course = course("Playback Course", "playback-course", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(7200));
    var media = mediaAsset(lesson, MediaProvider.BUNNY, MediaStatus.READY);

    mockMvc
        .perform(
            get("/learn/lessons/{lessonId}/playback", lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("BUNNY"))
        .andExpect(jsonPath("$.playbackId").value(media.getProviderAssetId()))
        .andExpect(jsonPath("$.playbackUrl").exists());
  }

  @Test
  void youtubeFreeLessonPlaybackWorksWithoutEnrollment() throws Exception {
    var creator = user("Creator", "creator-youtube@example.com");
    var student = user("Student", "student-youtube@example.com");
    var course = course("YouTube Course", "youtube-course", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, true, ReleaseType.IMMEDIATE, null, null);
    var media = mediaAsset(lesson, MediaProvider.YOUTUBE, MediaStatus.READY);

    mockMvc
        .perform(
            get("/learn/lessons/{lessonId}/playback", lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("YOUTUBE"))
        .andExpect(jsonPath("$.playbackMode").value("IFRAME"))
        .andExpect(
            jsonPath("$.embedUrl")
                .value("https://www.youtube.com/embed/" + media.getProviderAssetId()))
        .andExpect(jsonPath("$.token").doesNotExist());
  }

  @Test
  void muxPaidLessonPlaybackReturnsSignedTokenForEnrolledStudent() throws Exception {
    var creator = user("Creator", "creator-mux@example.com");
    var student = user("Student", "student-mux@example.com");
    var course = course("Mux Course", "mux-course", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    var media = mediaAsset(lesson, MediaProvider.MUX, MediaStatus.READY);

    mockMvc
        .perform(
            get("/learn/lessons/{lessonId}/playback", lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("MUX"))
        .andExpect(jsonPath("$.playbackMode").value("HLS"))
        .andExpect(jsonPath("$.playbackId").value(media.getPlaybackId()))
        .andExpect(
            jsonPath("$.playbackUrl")
                .value(
                    org.hamcrest.Matchers.containsString(
                        "stream.mux.com/" + media.getPlaybackId())))
        .andExpect(jsonPath("$.token").isString())
        .andExpect(jsonPath("$.expiresAt").exists());
  }

  @Test
  void muxPaidLessonPlaybackRejectsWhenNotEnrolled() throws Exception {
    var creator = user("Creator", "creator-mux2@example.com");
    var student = user("Student", "student-mux2@example.com");
    var course = course("Mux Course 2", "mux-course-2", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    mediaAsset(lesson, MediaProvider.MUX, MediaStatus.READY);

    mockMvc
        .perform(
            get("/learn/lessons/{lessonId}/playback", lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isForbidden());
  }
}
