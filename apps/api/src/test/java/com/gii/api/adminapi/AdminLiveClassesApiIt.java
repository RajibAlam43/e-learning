package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.LiveClassRegistrantStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

class AdminLiveClassesApiIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void listLiveClassesShouldReadPersistedClassAndRegistrantCount() throws Exception {
    final var admin = user("Live Class Admin", "live-class-admin@example.com");
    var creator = user("Live Class Creator", "live-class-creator@example.com");
    var instructor = user("Live Class Instructor", "live-class-instructor@example.com");
    var firstStudent = user("First Student", "first-live-student@example.com");
    final var secondStudent = user("Second Student", "second-live-student@example.com");
    var course = course("Database Live Course", "database-live-course", creator);
    course.setTitleEn("Database Live Course English");
    courseRepository.saveAndFlush(course);
    var section = section(course, 1);
    var liveClass = liveClass(course, section, lesson(course, section, 1));
    liveClass.setTitleEn("Live Session English");
    liveClass.setInstructor(instructor);
    liveClassRepository.saveAndFlush(liveClass);
    registrant(liveClass, firstStudent, LiveClassRegistrantStatus.APPROVED);
    registrant(liveClass, secondStudent, LiveClassRegistrantStatus.PENDING);
    var completedClass = liveClass(course, section, lesson(course, section, 2));
    completedClass.setStatus(com.gii.common.enums.LiveClassStatus.COMPLETED);
    liveClassRepository.saveAndFlush(completedClass);
    liveClassRegistrantRepository.flush();

    mockMvc
        .perform(
            get("/admin/live-classes")
                .param("status", "SCHEDULED")
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].liveClassId").value(liveClass.getId().toString()))
        .andExpect(jsonPath("$.content[0].title").value("Live Session"))
        .andExpect(jsonPath("$.content[0].titleEn").value("Live Session English"))
        .andExpect(jsonPath("$.content[0].courseName").value("Database Live Course"))
        .andExpect(jsonPath("$.content[0].courseNameEn").value("Database Live Course English"))
        .andExpect(jsonPath("$.content[0].instructorName").value("Live Class Instructor"))
        .andExpect(jsonPath("$.content[0].status").value("SCHEDULED"))
        .andExpect(jsonPath("$.content[0].approvedRegistrants").value(1))
        .andExpect(jsonPath("$.content[0].registeredStudents").doesNotExist())
        .andExpect(jsonPath("$.content[0].startsAt").isNotEmpty())
        .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty())
        .andExpect(
            result ->
                assertThat(result.getResponse().getContentAsString())
                    .doesNotContain("hostStartUrl", "participantJoinUrl", "providerMeetingId"));
  }

  @Test
  void listLiveClassesShouldRejectNonAdminUsers() throws Exception {
    var user = user("Regular User", "regular-live-user@example.com");
    var studentAuthentication =
        new UsernamePasswordAuthenticationToken(
            user.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

    mockMvc
        .perform(get("/admin/live-classes").with(authentication(studentAuthentication)))
        .andExpect(status().isForbidden());
  }

  @Test
  void listLiveClassesShouldOrderSoonestClassFirst() throws Exception {
    var admin = user("Ordering Admin", "ordering-admin@example.com");
    var creator = user("Ordering Creator", "ordering-creator@example.com");
    var course = course("Ordering Course", "ordering-course", creator);
    var section = section(course, 1);
    var laterClass = liveClass(course, section, lesson(course, section, 1));
    laterClass.setStartsAt(Instant.parse("2030-01-02T10:00:00Z"));
    laterClass.setEndsAt(Instant.parse("2030-01-02T11:00:00Z"));
    laterClass = liveClassRepository.saveAndFlush(laterClass);
    var soonerClass = liveClass(course, section, lesson(course, section, 2));
    soonerClass.setStartsAt(Instant.parse("2030-01-01T10:00:00Z"));
    soonerClass.setEndsAt(Instant.parse("2030-01-01T11:00:00Z"));
    soonerClass = liveClassRepository.saveAndFlush(soonerClass);

    mockMvc
        .perform(
            get("/admin/live-classes")
                .param("status", "SCHEDULED")
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].liveClassId").value(soonerClass.getId().toString()))
        .andExpect(jsonPath("$.content[1].liveClassId").value(laterClass.getId().toString()));
  }

  @Test
  void completedLiveClassShouldRejectEnglishMetadataMutationByAdmin() throws Exception {
    var admin = user("Mutation Admin", "mutation-admin@example.com");
    var creator = user("Mutation Creator", "mutation-creator@example.com");
    var course = course("Mutation Course", "mutation-course", creator);
    var section = section(course, 1);
    var liveClass = liveClass(course, section, lesson(course, section, 1));
    liveClass.setStatus(com.gii.common.enums.LiveClassStatus.COMPLETED);
    liveClassRepository.saveAndFlush(liveClass);

    mockMvc
        .perform(
            patch("/live-classes/{liveClassId}", liveClass.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descriptionEn\":\"Rewritten completed description\"}"))
        .andExpect(status().isBadRequest());
  }
}
