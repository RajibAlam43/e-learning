package com.gii.api.instructorapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.api.service.live.GoogleMeetCredentialsService;
import com.gii.api.service.live.LiveMeetingCreateRequest;
import com.gii.api.service.live.oauth.GoogleMeetConnectionService;
import com.gii.api.service.live.oauth.GoogleMeetOauthClientService;
import com.gii.api.service.live.oauth.GoogleMeetTokenCipherService;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.integration.GoogleMeetConnectionRepository;
import com.gii.common.repository.integration.GoogleMeetOauthStateRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@TestPropertySource(
    properties = {
      "integrations.google-meet.shared-organizer-enabled=false",
      "integrations.google-meet.calendar-sync-enabled=true",
      "integrations.google-meet.token-encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
      "integrations.google-meet.oauth-redirect-uri=https://api.example.com/public/google-meet/oauth/callback",
      "integrations.google-meet.oauth-return-url="
    })
class GoogleMeetConnectionApiIt extends AbstractInstructorApiIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private GoogleMeetConnectionRepository connections;
  @Autowired private GoogleMeetOauthStateRepository states;
  @Autowired private GoogleMeetTokenCipherService cipher;
  @Autowired private GoogleMeetCredentialsService credentials;
  @MockitoBean private GoogleMeetOauthClientService oauth;

  @BeforeEach
  void setupGoogleConsent() {
    when(oauth.exchange(anyString(), anyString(), anyString()))
        .thenReturn(
            new GoogleMeetOauthClientService.Grant(
                "access",
                "private-refresh-token",
                "openid email " + GoogleMeetConnectionService.CALENDAR_SCOPE,
                "google-owner-subject",
                "teacher-google@gmail.com",
                null));
    when(oauth.refresh("private-refresh-token")).thenReturn("teacher-access");
  }

  @AfterEach
  void cleanup() {
    connections.deleteAll();
    states.deleteAll();
    cleanupInstructorData();
    ReflectionTestUtils.setField(credentials, "shared", false);
  }

  @Test
  void browserBoundConsentStoresEncryptedTokenAndCannotBeReplayed() throws Exception {
    UUID teacher = user("Teacher", "teacher-lms@example.com").getId();
    Attempt attempt = authorize(teacher);
    assertThat(attempt.url())
        .contains("access_type=offline", "code_challenge_method=S256", "nonce=");
    assertThat(attempt.cookie()).contains("HttpOnly", "Secure", "SameSite=Lax");
    mvc.perform(
            get(GoogleMeetConnectionService.CALLBACK)
                .param("state", attempt.state())
                .param("code", "code")
                .cookie(new Cookie(GoogleMeetConnectionService.COOKIE, "another-browser")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get(GoogleMeetConnectionService.CALLBACK)
                .param("state", attempt.state())
                .param("code", "code")
                .cookie(new Cookie(GoogleMeetConnectionService.COOKIE, attempt.browser())))
        .andExpect(status().isOk());
    var connection = connections.findByUserId(teacher).orElseThrow();
    assertThat(connection.getEmail()).isEqualTo("teacher-google@gmail.com");
    assertThat(connection.getEncryptedRefreshToken()).doesNotContain("private-refresh-token");
    assertThat(cipher.decrypt(connection.getEncryptedRefreshToken(), "refresh:" + teacher))
        .isEqualTo("private-refresh-token");
    mvc.perform(
            get(GoogleMeetConnectionService.CALLBACK)
                .param("state", attempt.state())
                .param("code", "code")
                .cookie(new Cookie(GoogleMeetConnectionService.COOKIE, attempt.browser())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void expiredAndDeniedConsentNeverCreatesConnection() throws Exception {
    UUID teacher = user("Teacher", "expired@example.com").getId();
    Attempt expired = authorize(teacher);
    var state = states.findAll().getFirst();
    state.setExpiresAt(Instant.now().minusSeconds(1));
    states.saveAndFlush(state);
    mvc.perform(
            get(GoogleMeetConnectionService.CALLBACK)
                .param("state", expired.state())
                .param("code", "code")
                .cookie(new Cookie(GoogleMeetConnectionService.COOKIE, expired.browser())))
        .andExpect(status().isBadRequest());
    Attempt denied = authorize(teacher);
    mvc.perform(
            get(GoogleMeetConnectionService.CALLBACK)
                .param("state", denied.state())
                .param("error", "access_denied")
                .cookie(new Cookie(GoogleMeetConnectionService.COOKIE, denied.browser())))
        .andExpect(status().isBadRequest());
    assertThat(connections.count()).isZero();
    verify(oauth, never()).exchange(any(), any(), any());
  }

  @Test
  void missingCalendarScopeDoesNotSaveTeacherCredentials() throws Exception {
    UUID teacher = user("Teacher", "scopes@example.com").getId();
    Attempt attempt = authorize(teacher);
    when(oauth.exchange(any(), any(), any()))
        .thenReturn(
            new GoogleMeetOauthClientService.Grant(
                "access", "refresh", "openid email", "subject", "teacher@gmail.com", null));
    mvc.perform(
            get(GoogleMeetConnectionService.CALLBACK)
                .param("state", attempt.state())
                .param("code", "code")
                .cookie(new Cookie(GoogleMeetConnectionService.COOKIE, attempt.browser())))
        .andExpect(status().isBadRequest());
    assertThat(connections.count()).isZero();
  }

  @Test
  void switchingToSharedModeDoesNotChangeTeacherMeetingCredentials() throws Exception {
    UUID teacher = user("Teacher", "owner@example.com").getId();
    Attempt attempt = authorize(teacher);
    mvc.perform(
            get(GoogleMeetConnectionService.CALLBACK)
                .param("state", attempt.state())
                .param("code", "code")
                .cookie(new Cookie(GoogleMeetConnectionService.COOKIE, attempt.browser())))
        .andExpect(status().isOk());
    ReflectionTestUtils.setField(credentials, "shared", true);
    var metadata =
        Map.<String, Object>of(
            "googleOrganizerMode",
            "TEACHER",
            "googleApiKind",
            "CALENDAR",
            "googleOwnerUserId",
            teacher.toString(),
            "googleOwnerSubject",
            "google-owner-subject");
    var owner = credentials.forMeeting(metadata);
    assertThat(owner.accessToken()).isEqualTo("teacher-access");
    assertThat(owner.userId()).isEqualTo(teacher);
    assertThat(owner.mode()).isEqualTo("TEACHER");
    var differentAccount = new java.util.HashMap<>(metadata);
    differentAccount.put("googleOwnerSubject", "another-subject");
    assertThatThrownBy(() -> credentials.forMeeting(differentAccount))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void assistantSchedulingUsesPrimaryTeacherAsOwnerWithoutRequiringOtherTeachersCoHosts()
      throws Exception {
    var primary = user("Primary", "primary@example.com");
    primary.setEmail(null);
    primary.setPhone("+8801700000001");
    userRepository.saveAndFlush(primary);
    var assistant = user("Assistant", "assistant@example.com");
    var course = course("Teacher-owned", "teacher-owned", assistant, PublishStatus.PUBLISHED);
    assignment(course, primary, InstructorRole.PRIMARY);
    assignment(course, assistant, InstructorRole.ASSISTANT);
    var slot = liveClassItem(section(course, 1, PublishStatus.PUBLISHED), 1);
    Instant starts = Instant.now().plusSeconds(3600);
    mvc.perform(
            post("/live-classes/courses/{id}", course.getId())
                .with(authentication(instructorAuth(assistant.getId())))
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
                                starts.plusSeconds(1800).toString()))))
        .andExpect(status().isOk());
    var request = ArgumentCaptor.forClass(LiveMeetingCreateRequest.class);
    verify(liveMeetingProvisioningService).createMeeting(request.capture());
    assertThat(request.getValue().primaryTeacherId()).isEqualTo(primary.getId());
    assertThat(request.getValue().coHostEmails()).isEmpty();
    assertThat(liveClassRepository.findAll().getFirst().getLeadInstructor().getId())
        .isEqualTo(primary.getId());
  }

  private Attempt authorize(UUID teacher) throws Exception {
    var response =
        mvc.perform(
                post("/instructor/google-meet/connection/authorize")
                    .with(authentication(instructorAuth(teacher))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    String url =
        new ObjectMapper()
            .readTree(response.getContentAsString())
            .path("authorizationUrl")
            .asText();
    String state =
        UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("state");
    String cookie = response.getHeader("Set-Cookie");
    return new Attempt(url, state, cookie.split(";", 2)[0].split("=", 2)[1], cookie);
  }

  private record Attempt(String url, String state, String browser, String cookie) {}
}
