package com.gii.api.lessonapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class LessonResourcesApiIT extends AbstractLessonApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private R2PresignedUrlService r2PresignedUrlService;

  @AfterEach
  void cleanup() {
    cleanupLessonData();
  }

  @Test
  void resourcesAndDownloadUrlReturnForAccessibleLesson() throws Exception {
    var creator = user("Creator", "creator-rsrc@example.com");
    var student = user("Student", "student-rsrc@example.com");
    var course = course("Course R", "course-r", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson =
        lesson(course, sec, 1, PublishStatus.PUBLISHED, false, ReleaseType.IMMEDIATE, null, null);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(3600));
    var resourceA = resource(lesson, 2, "Appendix");
    var resourceB = resource(lesson, 1, "Workbook");
    when(r2PresignedUrlService.generateDownloadUrl(any(), any(), any()))
        .thenReturn(
            new R2PresignedUrlService.PresignedDownload(
                "https://signed.test/file", Instant.now().plusSeconds(600)));

    mockMvc
        .perform(
            get("/learn/lessons/{lessonId}/resources", lesson.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].resourceId").value(resourceB.getId().toString()))
        .andExpect(jsonPath("$[1].resourceId").value(resourceA.getId().toString()));

    mockMvc
        .perform(
            get("/learn/resources/{resourceId}/download-url", resourceB.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.downloadUrl").value("https://signed.test/file"))
        .andExpect(jsonPath("$.fileName").value("Workbook"));
  }
}
