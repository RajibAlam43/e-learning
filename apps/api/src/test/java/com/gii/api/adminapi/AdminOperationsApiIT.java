package com.gii.api.adminapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
            post("/admin/courses/{courseId}/live-classes", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Live Session 1",
                      "sectionId":"%s",
                      "lessonId":"%s",
                      "startsAt":"%s",
                      "endsAt":"%s",
                      "zoomMeetingLink":"https://zoom.test/join/1"
                    }
                    """
                        .formatted(
                            sec.getId(),
                            lesson.getId(),
                            Instant.now().plusSeconds(3600),
                            Instant.now().plusSeconds(7200))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Live Session 1"));

    var live = liveClassRepository.findAll().getFirst();
    mockMvc
        .perform(
            post("/admin/live-classes/{liveClassId}/start", live.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LIVE"));

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/quizzes", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
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
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Quiz 1"));

    var quiz = quizRepository.findAll().getFirst();
    mockMvc
        .perform(
            post("/admin/quizzes/{quizId}/publish", quiz.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());

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
}
