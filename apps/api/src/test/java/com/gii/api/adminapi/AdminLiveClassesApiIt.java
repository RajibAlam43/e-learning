package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.LiveClassRegistrantStatus;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    liveClassRegistrantRepository.flush();

    mockMvc
        .perform(get("/admin/live-classes").with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].liveClassId").value(liveClass.getId().toString()))
        .andExpect(jsonPath("$[0].title").value("Live Session"))
        .andExpect(jsonPath("$[0].titleEn").value("Live Session English"))
        .andExpect(jsonPath("$[0].courseName").value("Database Live Course"))
        .andExpect(jsonPath("$[0].courseNameEn").value("Database Live Course English"))
        .andExpect(jsonPath("$[0].instructorName").value("Live Class Instructor"))
        .andExpect(jsonPath("$[0].status").value("SCHEDULED"))
        .andExpect(jsonPath("$[0].registeredStudents").value(2))
        .andExpect(jsonPath("$[0].startsAt").isNotEmpty())
        .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
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
}
