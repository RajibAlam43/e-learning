package com.gii.api.instructorapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.api.service.live.GoogleMeetCredentialsService;
import com.gii.api.service.live.GoogleMeetLiveMeetingProvider;
import com.gii.api.service.live.LiveClassSchedulingService;
import com.gii.api.service.live.LiveMeetingCreateRequest;
import com.gii.api.service.live.LiveMeetingCreateResult;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassSlot;
import com.gii.common.entity.user.User;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

class GoogleMeetSchedulingApiIt extends AbstractInstructorApiIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private GoogleMeetCredentialsService credentials;
  @MockitoBean private GoogleMeetLiveMeetingProvider google;

  @BeforeEach
  void setupGoogle() {
    when(google.synchronizeCoHosts(anyString(), anyList(), anyMap()))
        .thenAnswer(call -> new HashMap<>(call.<Map<String, Object>>getArgument(2)));
    when(google.create(any())).thenReturn(meeting("upgraded"));
    when(liveMeetingProvisioningService.createMeeting(any()))
        .thenAnswer(call -> meeting(UUID.randomUUID().toString()));
  }

  @AfterEach
  void cleanup() {
    cleanupInstructorData();
    ReflectionTestUtils.setField(credentials, "shared", true);
  }

  @Test
  void teacherModeRejectsLegacyCoHostUpgradeButRetainsExistingSharedSpaceRoles() throws Exception {
    ReflectionTestUtils.setField(credentials, "shared", false);
    var legacy = googleClass("legacy-teacher-mode", "old-calendar-id");
    User admin = user("Admin", "admin-teacher-mode@example.com");
    mvc.perform(
            post("/admin/live-classes/{id}/co-hosts", legacy.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isConflict());
    verify(google, never()).create(any());
    var sharedSpace = googleClass("existing-shared", "spaces/existing");
    mvc.perform(
            post("/admin/live-classes/{id}/co-hosts", sharedSpace.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
  }

  @Test
  void differentLeadTeachersCanScheduleSimultaneouslyWithSharedCourseCoHosts() throws Exception {
    User first = user("First teacher", "first-meet@example.com");
    User second = user("Second teacher", "second-meet@example.com");
    User assistant = user("Shared assistant", "assistant-meet@example.com");
    Course one = course("One", "meet-one", first, PublishStatus.PUBLISHED);
    Course two = course("Two", "meet-two", second, PublishStatus.PUBLISHED);
    assignment(one, first, InstructorRole.PRIMARY);
    assignment(one, assistant, InstructorRole.ASSISTANT);
    assignment(two, second, InstructorRole.PRIMARY);
    assignment(two, assistant, InstructorRole.ASSISTANT);
    var slotOne = liveClassItem(section(one, 1, PublishStatus.PUBLISHED), 1);
    var slotTwo = liveClassItem(section(two, 1, PublishStatus.PUBLISHED), 1);
    Instant starts = Instant.now().plusSeconds(3600);
    create(one, slotOne, first, starts, starts.plusSeconds(1800), 200);
    create(two, slotTwo, second, starts, starts.plusSeconds(1800), 200);
    Course three = course("Three", "meet-three", first, PublishStatus.PUBLISHED);
    assignment(three, first, InstructorRole.PRIMARY);
    var slotThree = liveClassItem(section(three, 1, PublishStatus.PUBLISHED), 1);
    create(three, slotThree, first, starts, starts.plusSeconds(1800), 400);
    UUID later =
        create(three, slotThree, first, starts.plusSeconds(1800), starts.plusSeconds(3600), 200);
    mvc.perform(
            patch("/live-classes/{id}", later)
                .with(authentication(instructorAuth(first.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    new ObjectMapper()
                        .writeValueAsString(
                            Map.of(
                                "startsAt",
                                starts.toString(),
                                "endsAt",
                                starts.plusSeconds(1500).toString()))))
        .andExpect(status().isBadRequest());
    assertThat(liveClassRepository.findAll()).hasSize(3);
  }

  @Test
  void adminSchedulingMakesAllCourseTeachersCoHostsAndUsesPrimaryAsLead() throws Exception {
    User admin = user("Admin", "admin-meet@example.com");
    User first = user("Primary", "primary-meet@example.com");
    User second = user("Assistant", "second-meet@example.com");
    Course course = course("Admin scheduled", "admin-meet", admin, PublishStatus.PUBLISHED);
    assignment(course, first, InstructorRole.PRIMARY);
    assignment(course, second, InstructorRole.ASSISTANT);
    var slot = liveClassItem(section(course, 1, PublishStatus.PUBLISHED), 1);
    Instant starts = Instant.now().plusSeconds(3600);
    mvc.perform(
            post(
                    "/admin/courses/{courseId}/live-classes/{slotId}/schedule",
                    course.getId(),
                    slot.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    new ObjectMapper()
                        .writeValueAsString(
                            Map.of(
                                "provider",
                                "GOOGLE_MEET",
                                "maxCapacity",
                                100,
                                "startsAt",
                                starts.toString(),
                                "endsAt",
                                starts.plusSeconds(1800).toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.instructorId").value(first.getId().toString()));
    ArgumentCaptor<LiveMeetingCreateRequest> request =
        ArgumentCaptor.forClass(LiveMeetingCreateRequest.class);
    verify(liveMeetingProvisioningService).createMeeting(request.capture());
    assertThat(request.getValue().coHostEmails())
        .containsExactlyInAnyOrder(first.getEmail(), second.getEmail());
  }

  @Test
  void adminCanAddSelfAndAnotherHostAndBothSurviveStartSynchronization() throws Exception {
    var live = googleClass("manual", "spaces/manual");
    User admin = user("Admin", "admin-manual@example.com");
    mvc.perform(
            post("/admin/live-classes/{id}/co-hosts", live.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(admin.getEmail()))
        .andExpect(jsonPath("$.role").value("COHOST"));
    for (int attempt = 0; attempt < 2; attempt++) {
      mvc.perform(
              post("/admin/live-classes/{id}/co-hosts", live.getId())
                  .with(authentication(adminAuth(admin.getId())))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"email\":\"guest@example.com\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.coHostEmails.length()").value(3));
    }
    mvc.perform(
            post("/live-classes/{id}/start", live.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hostStartUrl").value("https://meet.google.com/abc-defg-hij"));
    var saved = liveClassRepository.findById(live.getId()).orElseThrow();
    assertThat(saved.getProviderMetadata())
        .containsEntry(
            LiveClassSchedulingService.MANUAL_HOSTS,
            List.of(admin.getEmail(), "guest@example.com"));
    assertThat(saved.getStatus()).isEqualTo(LiveClassStatus.LIVE);
  }

  @Test
  void manualHostEndpointEnforcesAdminRoleAndRollsBackOnGoogleFailure() throws Exception {
    var live = googleClass("failure", "spaces/failure");
    User admin = user("Admin", "admin-failure@example.com");
    mvc.perform(
            post("/admin/live-classes/{id}/co-hosts", live.getId())
                .with(authentication(instructorAuth(admin.getId()))))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/admin/live-classes/{id}/co-hosts", live.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\"}"))
        .andExpect(status().isBadRequest());
    when(google.synchronizeCoHosts(anyString(), anyList(), anyMap()))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google unavailable"));
    mvc.perform(
            post("/admin/live-classes/{id}/co-hosts", live.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isBadGateway());
    var saved = liveClassRepository.findById(live.getId()).orElseThrow();
    assertThat(saved.getProviderMetadata())
        .doesNotContainKey(LiveClassSchedulingService.MANUAL_HOSTS);
    assertThat(saved.getStatus()).isEqualTo(LiveClassStatus.SCHEDULED);
  }

  @Test
  void scheduledLegacyClassGetsNewMeetResourceAndRetainsCalendarReferenceForCleanup()
      throws Exception {
    var live = googleClass("legacy", "old-calendar-id");
    User admin = user("Admin", "admin-legacy@example.com");
    mvc.perform(
            post("/admin/live-classes/{id}/co-hosts", live.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
    var saved = liveClassRepository.findById(live.getId()).orElseThrow();
    assertThat(saved.getProviderMeetingId()).isEqualTo("spaces/upgraded");
    assertThat(saved.getProviderMetadata())
        .containsEntry("legacyCalendarEventId", "old-calendar-id");
    ArgumentCaptor<LiveMeetingCreateRequest> request =
        ArgumentCaptor.forClass(LiveMeetingCreateRequest.class);
    verify(google).create(request.capture());
    assertThat(request.getValue().coHostEmails()).contains(admin.getEmail(), "legacy@example.com");
  }

  @Test
  void activeLegacyClassDoesNotSilentlySwitchMeetingLinks() throws Exception {
    var live = googleClass("active-legacy", "old-calendar-id");
    live.setStatus(LiveClassStatus.LIVE);
    liveClassRepository.saveAndFlush(live);
    User admin = user("Admin", "admin-active@example.com");
    mvc.perform(
            post("/admin/live-classes/{id}/co-hosts", live.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isConflict());
    assertThat(liveClassRepository.findById(live.getId()).orElseThrow().getProviderMeetingId())
        .isEqualTo("old-calendar-id");
  }

  @Test
  void manualHostsCannotPushCombinedListAboveTwentyFive() throws Exception {
    var live = googleClass("limit", "spaces/limit");
    List<String> manual =
        IntStream.range(0, 24).mapToObj(i -> "host" + i + "@example.com").toList();
    live.setProviderMetadata(Map.of(LiveClassSchedulingService.MANUAL_HOSTS, manual));
    liveClassRepository.saveAndFlush(live);
    User admin = user("Admin", "admin-limit@example.com");
    mvc.perform(
            post("/admin/live-classes/{id}/co-hosts", live.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isBadRequest());
    assertThat(liveClassRepository.findById(live.getId()).orElseThrow().getProviderMetadata())
        .containsEntry(LiveClassSchedulingService.MANUAL_HOSTS, manual);
  }

  private LiveClass googleClass(String name, String meetingId) {
    User teacher = user("Teacher", name + "@example.com");
    Course course = course(name, name + "-course", teacher, PublishStatus.PUBLISHED);
    assignment(course, teacher, InstructorRole.PRIMARY);
    var section = section(course, 1, PublishStatus.PUBLISHED);
    LiveClass live =
        liveClass(
            course,
            section,
            null,
            teacher,
            LiveClassStatus.SCHEDULED,
            Instant.now().plusSeconds(3600),
            Instant.now().plusSeconds(5400));
    live.setProvider(LiveClassProvider.GOOGLE_MEET);
    live.setLeadInstructor(teacher);
    live.setProviderMeetingId(meetingId);
    live.setHostStartUrl("https://calendar.google.com/calendar/event?eid=old");
    live.setParticipantJoinUrl("https://meet.google.com/abc-defg-hij");
    live.setProviderMetadata(Map.of());
    return liveClassRepository.saveAndFlush(live);
  }

  private UUID create(
      Course course, LiveClassSlot slot, User teacher, Instant starts, Instant ends, int expected)
      throws Exception {
    String result =
        mvc.perform(
                post("/live-classes/courses/{id}", course.getId())
                    .with(authentication(instructorAuth(teacher.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        new ObjectMapper()
                            .writeValueAsString(
                                Map.of(
                                    "liveClassItemId",
                                    slot.getId().toString(),
                                    "title",
                                    "Class",
                                    "provider",
                                    "GOOGLE_MEET",
                                    "maxCapacity",
                                    100,
                                    "startsAt",
                                    starts.toString(),
                                    "endsAt",
                                    ends.toString()))))
            .andExpect(status().is(expected))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return expected == 200
        ? UUID.fromString(new ObjectMapper().readTree(result).path("liveClassId").asText())
        : null;
  }

  private LiveMeetingCreateResult meeting(String id) {
    return LiveMeetingCreateResult.builder()
        .meetingId("spaces/" + id)
        .hostStartUrl("https://meet.google.com/abc-defg-hij")
        .participantJoinUrl("https://meet.google.com/abc-defg-hij")
        .metadata(Map.of())
        .build();
  }
}
