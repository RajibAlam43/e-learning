package com.gii.api.adminapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

class AdminOperationsApiIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void mediaInstructorLiveQuizAndOrderFlowsShouldWork() throws Exception {
    var admin = user("Admin Ops", "admin-ops@example.com");
    var creator = user("Creator Ops", "creator-ops@example.com");
    var existingInstructorUser = user("Instructor Ops", "inst-ops@example.com");
    instructorProfile(existingInstructorUser);
    ensureInstructorRolePresent();

    var course = course("Ops Course", "ops-course", creator);
    var sec = section(course, 1);
    var lesson = lesson(course, sec, 1);

    mockMvc
        .perform(
            post("/admin/sections/{sectionId}/publish", sec.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
    org.assertj.core.api.Assertions.assertThat(
            courseSectionRepository.findById(sec.getId()).orElseThrow().getStatus())
        .isEqualTo(PublishStatus.PUBLISHED);

    mockMvc
        .perform(
            post("/admin/sections/{sectionId}/unpublish", sec.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
    org.assertj.core.api.Assertions.assertThat(
            courseSectionRepository.findById(sec.getId()).orElseThrow().getStatus())
        .isEqualTo(PublishStatus.DRAFT);

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/publish", lesson.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
    org.assertj.core.api.Assertions.assertThat(
            lessonRepository.findById(lesson.getId()).orElseThrow().getStatus())
        .isEqualTo(PublishStatus.PUBLISHED);

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/unpublish", lesson.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
    org.assertj.core.api.Assertions.assertThat(
            lessonRepository.findById(lesson.getId()).orElseThrow().getStatus())
        .isEqualTo(PublishStatus.DRAFT);

    mockMvc
        .perform(
            post("/admin/media-assets")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "lessonId":"%s",
                      "provider":"MUX",
                      "assetType":"VIDEO",
                      "providerAssetId":"asset-1",
                      "playbackId":"play-1",
                      "title":"Intro Video"
                    }
                    """
                        .formatted(lesson.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.playbackId").value("play-1"));

    var asset = mediaAssetRepository.findByLessonId(lesson.getId()).orElseThrow();
    mockMvc
        .perform(
            patch("/admin/media-assets/{mediaAssetId}", asset.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated Intro\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Intro"));

    mockMvc
        .perform(
            post("/admin/instructors")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"fullName":"New Instructor","email":"new-instructor@example.com","displayName":"New Inst"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("New Instructor"));

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/instructors", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"instructorUserId":"%s","role":"PRIMARY"}
                    """
                        .formatted(existingInstructorUser.getId())))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Live Session 1",
                      "sectionId":"%s",
                      "startsAt":"%s",
                      "endsAt":"%s",
                      "provider":"ZOOM",
                      "maxCapacity":100
                    }
                    """
                        .formatted(
                            sec.getId(),
                            Instant.now().plusSeconds(3600),
                            Instant.now().plusSeconds(7200))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Live Session 1"));

    var live = liveClassRepository.findAll().getFirst();
    clearInvocations(liveMeetingProvisioningService);
    mockMvc
        .perform(
            patch("/live-classes/{liveClassId}", live.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Live Session 1 Updated\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Live Session 1 Updated"));
    verify(liveMeetingProvisioningService, atLeastOnce()).updateMeeting(any());

    mockMvc
        .perform(
            delete("/live-classes/{liveClassId}", live.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
    verify(liveMeetingProvisioningService, atLeastOnce()).cancelMeeting(any());

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Live Session 2",
                      "sectionId":"%s",
                      "startsAt":"%s",
                      "endsAt":"%s",
                      "provider":"ZOOM",
                      "maxCapacity":100
                    }
                    """
                        .formatted(
                            sec.getId(),
                            Instant.now().plusSeconds(4000),
                            Instant.now().plusSeconds(7600))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Live Session 2"));

    live = liveClassRepository.findAll().stream()
        .filter(x -> "Live Session 2".equals(x.getTitle()))
        .findFirst()
        .orElseThrow();
    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/start", live.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LIVE"));

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/start", live.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/admin/sections/{sectionId}/quizzes", sec.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sectionId":"%s",
                      "position":2,
                      "title":"Quiz 1",
                      "passingScorePct":70,
                      "maxAttempts":2,
                      "timeLimitSec":900,
                      "questions":[
                        {"position":1,"questionText":"Q1","questionType":"MCQ","points":1,"choices":[
                          {"choiceText":"A","isCorrect":true},
                          {"choiceText":"B","isCorrect":false}
                        ]}
                      ]
                    }
                    """
                        .formatted(sec.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Quiz 1"));

    var quiz = quizRepository.findAll().getFirst();
    mockMvc
        .perform(
            post("/admin/quizzes/{quizId}/publish", quiz.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
    org.assertj.core.api.Assertions.assertThat(quizRepository.findById(quiz.getId()).orElseThrow().getStatus())
        .isEqualTo(PublishStatus.PUBLISHED);

    mockMvc
        .perform(
            post("/admin/quizzes/{quizId}/unpublish", quiz.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
    org.assertj.core.api.Assertions.assertThat(quizRepository.findById(quiz.getId()).orElseThrow().getStatus())
        .isEqualTo(PublishStatus.DRAFT);

    var buyer = user("Buyer One", "buyer-one@example.com");
    var order = order(buyer, OrderStatus.PENDING);
    orderItem(order, course, BigDecimal.valueOf(1500), BigDecimal.valueOf(300));

    mockMvc
        .perform(get("/admin/orders").with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].orderId").value(order.getId().toString()));

    mockMvc
        .perform(
            get("/admin/orders/{orderId}", order.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].finalAmount").value(1200));

    mockMvc
        .perform(
            patch("/admin/orders/{orderId}", order.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAID\",\"adminNote\":\"manually approved\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"));

    org.assertj.core.api.Assertions.assertThat(
            orderRepository.findById(order.getId()).orElseThrow().getPaidAt())
        .isNotNull();
  }

  @Test
  void adminCreateRejectsOverlappingForSameProviderAndAllowsGoogleMeetParallelWindow()
      throws Exception {
    var admin = user("Admin Overlap", "admin-overlap@example.com");
    var creator = user("Creator Overlap", "creator-overlap@example.com");
    var course = course("Overlap Course", "overlap-course", creator);
    var sec = section(course, 1);

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Zoom-1",
                      "sectionId":"%s",
                      "startsAt":"%s",
                      "endsAt":"%s",
                      "provider":"ZOOM",
                      "maxCapacity":100
                    }
                    """
                        .formatted(
                            sec.getId(),
                            Instant.now().plusSeconds(3600),
                            Instant.now().plusSeconds(5400))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Zoom-Overlap",
                      "sectionId":"%s",
                      "startsAt":"%s",
                      "endsAt":"%s",
                      "provider":"ZOOM",
                      "maxCapacity":100
                    }
                    """
                        .formatted(
                            sec.getId(),
                            Instant.now().plusSeconds(4200),
                            Instant.now().plusSeconds(6200))))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Meet-Same-Window",
                      "sectionId":"%s",
                      "startsAt":"%s",
                      "endsAt":"%s",
                      "provider":"GOOGLE_MEET",
                      "maxCapacity":100
                    }
                    """
                        .formatted(
                            sec.getId(),
                            Instant.now().plusSeconds(4200),
                            Instant.now().plusSeconds(6200))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("GOOGLE_MEET"));
  }

  @Test
  void adminCreateReturnsBadGatewayWhenProviderProvisioningFails() throws Exception {
    var admin = user("Admin Provider", "admin-provider@example.com");
    var creator = user("Creator Provider", "creator-provider@example.com");
    var course = course("Provider Course", "provider-course", creator);
    var sec = section(course, 1);

    when(liveMeetingProvisioningService.createMeeting(any()))
        .thenThrow(
            new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google Calendar API unavailable"));

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Provider Fail",
                      "sectionId":"%s",
                      "startsAt":"%s",
                      "endsAt":"%s",
                      "provider":"GOOGLE_MEET",
                      "maxCapacity":100
                    }
                    """
                        .formatted(
                            sec.getId(),
                            Instant.now().plusSeconds(3600),
                            Instant.now().plusSeconds(5400))))
        .andExpect(status().isBadGateway());
    org.assertj.core.api.Assertions.assertThat(liveClassRepository.count()).isZero();
  }

  @Test
  void sharedLiveClassEndpointsRejectStudentAndEnforceStatusTransitions() throws Exception {
    var admin = user("Admin Matrix", "admin-matrix@example.com");
    var creator = user("Creator Matrix", "creator-matrix@example.com");
    var student = user("Student Matrix", "student-matrix@example.com");
    var course = course("Matrix Course", "matrix-course", creator);
    var sec = section(course, 1);

    String body =
        """
        {
          "title":"Matrix Class",
          "sectionId":"%s",
          "startsAt":"%s",
          "endsAt":"%s",
          "provider":"ZOOM",
          "maxCapacity":100
        }
        """
            .formatted(
                sec.getId(),
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(5400));

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());
    var live = liveClassRepository.findAll().getFirst();

    mockMvc
        .perform(
            patch("/live-classes/{liveClassId}", live.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/start", live.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/live-classes/{liveClassId}", live.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
        .andExpect(status().isBadRequest());
  }

  private Authentication studentAuth(java.util.UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }
}
