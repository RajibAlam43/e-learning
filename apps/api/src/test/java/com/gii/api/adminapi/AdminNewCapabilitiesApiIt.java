package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.entity.quiz.QuizAttempt;
import com.gii.common.entity.user.UserRole;
import com.gii.common.entity.user.UserRoleId;
import com.gii.common.enums.SectionItemType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@TestPropertySource(
    properties = {
      "storage.r2.account-id=test-account",
      "storage.r2.access-key-id=test-access",
      "storage.r2.secret-access-key=test-secret",
      "storage.r2.bucket=test-bucket"
    })
class AdminNewCapabilitiesApiIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void adminCanUploadCreateUpdateAndDeleteLessonResourceMetadata() throws Exception {
    var admin = user("Resource Admin", "resource-admin@example.com");
    var creator = user("Creator", "resource-creator@example.com");
    var course = course("Resources", "resources", creator);
    var section = section(course, 1);
    var lesson = lesson(course, section, 1);

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/resources/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "filename":"handout.pdf",
                      "contentType":"application/pdf",
                      "sizeBytes":1024
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.objectKey")
                .value(
                    org.hamcrest.Matchers.matchesPattern(
                        "lesson-resources/" + lesson.getId() + "/handout-[0-9a-f]{8}\\.pdf")))
        .andExpect(jsonPath("$.method").value("PUT"))
        .andExpect(jsonPath("$.uploadUrl").isNotEmpty());

    String objectKey = "lesson-resources/" + lesson.getId() + "/handout-abcdef12.pdf";
    String createResponse =
        mockMvc
            .perform(
                post("/admin/lessons/{lessonId}/resources", lesson.getId())
                    .with(authentication(adminAuth(admin.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "title":"Handout",
                          "titleEn":"Handout EN",
                          "resourceType":"PDF",
                          "mimeType":"application/pdf",
                          "objectKey":"%s",
                          "position":1
                        }
                        """
                            .formatted(objectKey)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.objectKey").value(objectKey))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID resourceId =
        UUID.fromString(
            com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(createResponse)
                .get("resourceId")
                .asText());

    mockMvc
        .perform(
            patch("/admin/lesson-resources/{resourceId}", resourceId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Updated handout"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated handout"));

    mockMvc
        .perform(
            get("/admin/lessons/{lessonId}", lesson.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lessonId").value(lesson.getId().toString()))
        .andExpect(jsonPath("$.primaryResource").doesNotExist())
        .andExpect(jsonPath("$.resources[0].resourceId").value(resourceId.toString()))
        .andExpect(jsonPath("$.resources[0].title").value("Updated handout"));

    mockMvc
        .perform(
            get("/admin/lesson-resources/{resourceId}/download-url", resourceId)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.downloadUrl").isNotEmpty())
        .andExpect(jsonPath("$.fileName").value("Updated handout.pdf"));

    mockMvc
        .perform(
            get("/admin/lessons/{lessonId}", lesson.getId())
                .with(authentication(studentAuthentication())))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/admin/lesson-resources/{resourceId}/download-url", resourceId)
                .with(authentication(studentAuthentication())))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/resources", lesson.getId())
                .with(authentication(studentAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Leaked",
                      "resourceType":"PDF",
                      "mimeType":"application/pdf",
                      "objectKey":"%s",
                      "position":2
                    }
                    """
                        .formatted(objectKey)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete("/admin/lesson-resources/{resourceId}", resourceId)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNoContent());
    assertThat(lessonResourceRepository.existsById(resourceId)).isFalse();
  }

  @Test
  void lessonResourceUploadRejectsOversizeMimeMismatchAndForeignObjectKeys() throws Exception {
    var admin = user("Resource Edge Admin", "resource-edge-admin@example.com");
    var creator = user("Creator", "resource-edge-creator@example.com");
    var course = course("Resource Edge", "resource-edge", creator);
    var lesson = lesson(course, section(course, 1), 1);

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/resources/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "filename":"handout.pdf",
                      "contentType":"image/png",
                      "sizeBytes":1024
                    }
                    """))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/resources/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "filename":"handout.pdf",
                      "contentType":"application/pdf",
                      "sizeBytes":52428801
                    }
                    """))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/resources", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Foreign",
                      "resourceType":"PDF",
                      "mimeType":"application/pdf",
                      "objectKey":"lesson-resources/00000000-0000-0000-0000-000000000000/file-abcdef12.pdf",
                      "position":1
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void quizLessonCreationIsRejectedForStandaloneSectionQuiz() throws Exception {
    var admin = user("Structure Admin", "structure-admin@example.com");
    var creator = user("Creator", "structure-creator@example.com");
    var course = course("Structure", "structure", creator);
    var section = section(course, 1);

    mockMvc
        .perform(
            post("/admin/sections/{sectionId}/lessons", section.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Legacy quiz lesson",
                      "slug":"legacy-quiz-lesson",
                      "position":1,
                      "lessonType":"QUIZ"
                    }
                    """))
        .andExpect(status().isBadRequest());

    assertThat(lessonRepository.count()).isZero();
    assertThat(sectionItemRepository.count()).isZero();
  }

  @Test
  void quizDeletePreservesAttemptHistoryAndDeletesOnlyAttemptFreeQuiz() throws Exception {
    var admin = user("Quiz Admin", "delete-quiz-admin@example.com");
    final var student = user("Student", "delete-quiz-student@example.com");
    var creator = user("Creator", "delete-quiz-creator@example.com");
    var course = course("Quiz Delete", "quiz-delete", creator);
    section(course, 1);
    var deletable = quiz(course, "No attempts");

    mockMvc
        .perform(
            delete("/admin/quizzes/{quizId}", deletable.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNoContent());
    assertThat(quizRepository.existsById(deletable.getId())).isFalse();
    assertThat(
            sectionItemRepository.findByItemTypeAndItemId(SectionItemType.QUIZ, deletable.getId()))
        .isEmpty();

    var protectedQuiz = quiz(course, "Has attempt");
    Instant attemptStartedAt = Instant.now().minusSeconds(1);
    quizAttemptRepository.save(
        QuizAttempt.builder()
            .quiz(protectedQuiz)
            .user(student)
            .attemptNo(1)
            .scorePct(50)
            .passed(false)
            .startedAt(attemptStartedAt)
            .submittedAt(Instant.now())
            .build());

    mockMvc
        .perform(
            delete("/admin/quizzes/{quizId}", protectedQuiz.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isConflict());
    assertThat(quizRepository.existsById(protectedQuiz.getId())).isTrue();
    assertThat(quizAttemptRepository.existsByQuizId(protectedQuiz.getId())).isTrue();
  }

  @Test
  void instructorDeleteRemovesCapabilityButPreservesUserAccount() throws Exception {
    var admin = user("Instructor Admin", "delete-instructor-admin@example.com");
    var instructor = user("Instructor", "delete-instructor@example.com");
    var creator = user("Creator", "delete-instructor-creator@example.com");
    var course = course("Instructor Course", "instructor-course", creator);
    final var profile = instructorProfile(instructor);
    assignment(course, instructor);
    var role = roleRepository.findByName("INSTRUCTOR").orElseThrow();
    userRoleRepository.save(
        UserRole.builder()
            .id(UserRoleId.builder().userId(instructor.getId()).roleId(role.getId()).build())
            .user(instructor)
            .role(role)
            .build());

    mockMvc
        .perform(
            delete("/admin/instructors/{instructorId}", instructor.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNoContent());

    assertThat(userRepository.existsById(instructor.getId())).isTrue();
    assertThat(instructorProfileRepository.existsById(profile.getUserId())).isFalse();
    assertThat(courseInstructorRepository.findByInstructorId(instructor.getId())).isEmpty();
    assertThat(userRoleRepository.existsByUserIdAndRoleName(instructor.getId(), "INSTRUCTOR"))
        .isFalse();
  }

  @Test
  void adminCanListCloseAndReopenSupportTicketsWhileStudentCannot() throws Exception {
    var admin = user("Support Admin", "support-admin@example.com");

    String created =
        mockMvc
            .perform(
                post("/public/support/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email":"customer@example.com",
                          "subject":"Payment help",
                          "message":"Please help"
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID ticketId =
        UUID.fromString(
            com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(created)
                .get("ticketId")
                .asText());

    mockMvc
        .perform(get("/admin/support/tickets").with(authentication(studentAuthentication())))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/admin/support/tickets").with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ticketId").value(ticketId.toString()))
        .andExpect(jsonPath("$[0].message").value("Please help"));

    mockMvc
        .perform(
            patch("/admin/support/tickets/{ticketId}", ticketId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"CLOSED"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"))
        .andExpect(jsonPath("$.closedAt").isNotEmpty());

    mockMvc
        .perform(
            patch("/admin/support/tickets/{ticketId}", ticketId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"OPEN"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.closedAt").doesNotExist());
  }

  private UsernamePasswordAuthenticationToken studentAuthentication() {
    return new UsernamePasswordAuthenticationToken(
        UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }
}
