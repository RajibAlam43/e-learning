package com.gii.api.instructorapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
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

class InstructorLiveClassesApiIt extends AbstractInstructorApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupInstructorData();
  }

  @Test
  void createStartUpdateDeleteFlowWorksForAssignedInstructor() throws Exception {
    var creator = user("Creator", "creator-inst-live@example.com");
    var instructor = user("Instructor", "inst-live@example.com");
    var course = course("Course Live", "course-live-inst", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    Instant startsAt = Instant.now().plusSeconds(3600);
    Instant endsAt = Instant.now().plusSeconds(5400);
    String createBody =
        """
        {
          "sectionId":"%s",
          "position":1,
          "title":" Weekly Session ",
          "description":"Live review",
          "startsAt":"%s",
          "endsAt":"%s",
          "provider":"ZOOM",
          "maxCapacity":100
        }
        """
            .formatted(sec.getId(), startsAt.toString(), endsAt.toString());

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Weekly Session"))
        .andExpect(jsonPath("$.position").value(1))
        .andExpect(jsonPath("$.status").value("SCHEDULED"))
        .andExpect(jsonPath("$.provider").value("ZOOM"));

    String liveClassId = liveClassRepository.findAll().get(0).getId().toString();
    org.assertj.core.api.Assertions.assertThat(
            sectionItemRepository
                .findByItemTypeAndItemId(
                    com.gii.common.enums.SectionItemType.LIVE_CLASS,
                    java.util.UUID.fromString(liveClassId))
                .orElseThrow()
                .getPosition())
        .isEqualTo(1);
    clearInvocations(liveMeetingProvisioningService);

    Instant updatedEndsAt = Instant.now().plusSeconds(7200);
    String patchBody =
        """
        {"title":"Updated Session","endsAt":"%s"}
        """
            .formatted(updatedEndsAt.toString());
    mockMvc
        .perform(
            patch("/live-classes/{liveClassId}", liveClassId)
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Session"));
    verify(liveMeetingProvisioningService, atLeastOnce()).updateMeeting(any());

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/start", liveClassId)
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LIVE"));

    mockMvc
        .perform(
            patch("/live-classes/{liveClassId}", liveClassId)
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));

    mockMvc
        .perform(
            delete("/live-classes/{liveClassId}", liveClassId)
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createRejectsPositionAlreadyUsedByAnotherSectionItem() throws Exception {
    var creator = user("Creator Position", "creator-position@example.com");
    var instructor = user("Instructor Position", "instructor-position@example.com");
    var course = course("Position Course", "position-course", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var section = section(course, 1, PublishStatus.PUBLISHED);
    lesson(course, section, 1, PublishStatus.PUBLISHED);

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sectionId":"%s",
                      "position":1,
                      "title":"Conflicting Live Class",
                      "startsAt":"%s",
                      "endsAt":"%s",
                      "provider":"ZOOM",
                      "maxCapacity":100
                    }
                    """
                        .formatted(
                            section.getId(),
                            Instant.now().plusSeconds(3600),
                            Instant.now().plusSeconds(7200))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void startDeleteAndCreateEnforceOwnershipAndStatusRules() throws Exception {
    var creator = user("Creator", "creator-inst-live2@example.com");
    var instructor = user("Instructor", "inst-live2@example.com");
    final var otherInstructor = user("Other Instructor", "other-inst-live2@example.com");
    var course = course("Course Two", "course-two-inst", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    var completed =
        liveClass(
            course,
            sec,
            lesson,
            instructor,
            LiveClassStatus.COMPLETED,
            Instant.now().minusSeconds(3600),
            Instant.now().minusSeconds(1800));

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/start", completed.getId())
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            patch("/live-classes/{liveClassId}", completed.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titleEn\":\"Rewritten completed title\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            delete("/live-classes/{liveClassId}", completed.getId())
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isBadRequest());

    String createBody =
        """
        {
          "sectionId":"%s",
          "title":"No Access",
          "startsAt":"%s",
          "endsAt":"%s",
          "provider":"ZOOM",
          "maxCapacity":100
        }
        """
            .formatted(
                sec.getId(),
                Instant.now().plusSeconds(1800).toString(),
                Instant.now().plusSeconds(3600).toString());
    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(instructorAuth(otherInstructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
        .andExpect(status().isForbidden());
  }

  @Test
  void cancelScheduledLiveClassWorksForOwnerInstructor() throws Exception {
    var creator = user("Creator 4", "creator-inst-live4@example.com");
    var instructor = user("Instructor 4", "inst-live4@example.com");
    var course = course("Course Four", "course-four-inst", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);

    String createBody =
        """
        {
          "sectionId":"%s",
          "title":"Cancelable Class",
          "startsAt":"%s",
          "endsAt":"%s",
          "provider":"ZOOM",
          "maxCapacity":50
        }
        """
            .formatted(
                sec.getId(),
                Instant.now().plusSeconds(3600).toString(),
                Instant.now().plusSeconds(5400).toString());

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
        .andExpect(status().isOk());

    var liveClassId =
        liveClassRepository.findAll().stream()
            .filter(live -> "Cancelable Class".equals(live.getTitle()))
            .findFirst()
            .orElseThrow()
            .getId();

    clearInvocations(liveMeetingProvisioningService);
    mockMvc
        .perform(
            delete("/live-classes/{liveClassId}", liveClassId)
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isOk());
    verify(liveMeetingProvisioningService, atLeastOnce()).cancelMeeting(any());
  }

  @Test
  void startLiveClassRejectsWhenMeetingProvisioningDataMissing() throws Exception {
    var creator = user("Creator 3", "creator-inst-live3@example.com");
    var instructor = user("Instructor 3", "inst-live3@example.com");
    var course = course("Course Three", "course-three-inst", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    var scheduled =
        liveClass(
            course,
            sec,
            lesson,
            instructor,
            LiveClassStatus.SCHEDULED,
            Instant.now().plusSeconds(3600),
            Instant.now().plusSeconds(5400));
    scheduled.setProviderMeetingId(null);
    liveClassRepository.save(scheduled);

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/start", scheduled.getId())
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createRejectsOverlappingClassForSameProviderAndAllowsNonOverlapping() throws Exception {
    var creator = user("Creator 5", "creator-inst-live5@example.com");
    var instructor = user("Instructor 5", "inst-live5@example.com");
    var course = course("Course Five", "course-five-inst", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);

    Instant firstStart = Instant.now().plusSeconds(3600);
    Instant firstEnd = Instant.now().plusSeconds(5400);
    String firstBody =
        """
        {
          "sectionId":"%s",
          "title":"First Window",
          "startsAt":"%s",
          "endsAt":"%s",
          "provider":"ZOOM",
          "maxCapacity":100
        }
        """
            .formatted(sec.getId(), firstStart, firstEnd);

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstBody))
        .andExpect(status().isOk());

    String overlappingBody =
        """
        {
          "sectionId":"%s",
          "title":"Overlap Window",
          "startsAt":"%s",
          "endsAt":"%s",
          "provider":"ZOOM",
          "maxCapacity":100
        }
        """
            .formatted(
                sec.getId(), Instant.now().plusSeconds(4500), Instant.now().plusSeconds(6200));

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(overlappingBody))
        .andExpect(status().isBadRequest());

    String nonOverlappingBody =
        """
        {
          "sectionId":"%s",
          "title":"Second Window",
          "startsAt":"%s",
          "endsAt":"%s",
          "provider":"ZOOM",
          "maxCapacity":100
        }
        """
            .formatted(
                sec.getId(), Instant.now().plusSeconds(8000), Instant.now().plusSeconds(9200));

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(nonOverlappingBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Second Window"));
  }

  @Test
  void createReturnsBadGatewayWhenProviderProvisioningFails() throws Exception {
    var creator = user("Creator 6", "creator-inst-live6@example.com");
    var instructor = user("Instructor 6", "inst-live6@example.com");
    var course = course("Course Six", "course-six-inst", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);

    when(liveMeetingProvisioningService.createMeeting(any()))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Zoom API unavailable"));

    String body =
        """
        {
          "sectionId":"%s",
          "title":"Provider Fails",
          "startsAt":"%s",
          "endsAt":"%s",
          "provider":"ZOOM",
          "maxCapacity":100
        }
        """
            .formatted(
                sec.getId(), Instant.now().plusSeconds(3600), Instant.now().plusSeconds(5400));

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadGateway());
    org.assertj.core.api.Assertions.assertThat(liveClassRepository.count()).isZero();
  }

  @Test
  void updateAndCancelRejectInvalidTransitionsAndPreserveStateOnProviderFailure() throws Exception {
    var creator = user("Creator 7", "creator-inst-live7@example.com");
    var instructor = user("Instructor 7", "inst-live7@example.com");
    var course = course("Course Seven", "course-seven-inst", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);

    String createBody =
        """
        {
          "sectionId":"%s",
          "title":"Transition Class",
          "startsAt":"%s",
          "endsAt":"%s",
          "provider":"ZOOM",
          "maxCapacity":30
        }
        """
            .formatted(
                sec.getId(), Instant.now().plusSeconds(3600), Instant.now().plusSeconds(5400));
    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
        .andExpect(status().isOk());
    var liveId = liveClassRepository.findAll().getFirst().getId();

    mockMvc
        .perform(
            patch("/live-classes/{liveClassId}", liveId)
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/start", liveId)
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/live-classes/{liveClassId}", liveId)
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
        .andExpect(status().isBadRequest());

    var liveTwo =
        liveClass(
            course,
            sec,
            null,
            instructor,
            LiveClassStatus.SCHEDULED,
            Instant.now().plusSeconds(7200),
            Instant.now().plusSeconds(9000));
    doThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Provider cancel failed"))
        .when(liveMeetingProvisioningService)
        .cancelMeeting(any());
    mockMvc
        .perform(
            delete("/live-classes/{liveClassId}", liveTwo.getId())
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isBadGateway());
    org.assertj.core.api.Assertions.assertThat(
            liveClassRepository.findById(liveTwo.getId()).orElseThrow().getStatus())
        .isEqualTo(LiveClassStatus.SCHEDULED);
  }

  @Test
  void sharedEndpointsEnforceRoleAccessAndKeepUtcInstants() throws Exception {
    var creator = user("Creator 8", "creator-inst-live8@example.com");
    var instructor = user("Instructor 8", "inst-live8@example.com");
    var student = user("Student 8", "student-inst-live8@example.com");
    var course = course("Course Eight", "course-eight-inst", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);

    Instant startsAt = Instant.parse("2036-07-01T10:00:00Z");
    Instant endsAt = Instant.parse("2036-07-01T11:00:00Z");
    String createBody =
        """
        {
          "sectionId":"%s",
          "title":"UTC Class",
          "startsAt":"%s",
          "endsAt":"%s",
          "provider":"ZOOM",
          "maxCapacity":40
        }
        """
            .formatted(sec.getId(), startsAt, endsAt);

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(instructorAuth(instructor.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.startsAt").value("2036-07-01T10:00:00Z"))
        .andExpect(jsonPath("$.endsAt").value("2036-07-01T11:00:00Z"));
  }

  private Authentication studentAuth(java.util.UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }
}
