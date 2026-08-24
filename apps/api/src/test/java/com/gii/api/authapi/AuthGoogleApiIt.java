package com.gii.api.authapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.api.service.auth.GoogleIdentityTokenVerifier;
import com.gii.api.service.auth.GoogleIdentityTokenVerifier.GoogleProfile;
import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class AuthGoogleApiIt extends AbstractAuthApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @MockitoBean private GoogleIdentityTokenVerifier googleTokenVerifier;

  @AfterEach
  void cleanup() {
    cleanAuthTables();
  }

  @Test
  void googleAuthenticationCreatesVerifiedPasswordlessStudentAndIssuesTokens() throws Exception {
    when(googleTokenVerifier.verify("new-google-token"))
        .thenReturn(new GoogleProfile("google-subject-1", "New.Student@Gmail.com", "New Student"));

    mockMvc
        .perform(
            post("/public/auth/google")
                .contentType(APPLICATION_JSON)
                .content("{\"credential\":\"new-google-token\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isVerified").value(true))
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.fullName").value("New Student"))
        .andExpect(jsonPath("$.roles[0]").value("STUDENT"))
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=")))
        .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));

    User created = userRepository.findByEmail("new.student@gmail.com").orElseThrow();
    assertThat(created.getPasswordHash()).isNull();
    assertThat(created.getGoogleSubject()).isEqualTo("google-subject-1");
    assertThat(created.getEmailVerifiedAt()).isNotNull();
    assertThat(userRoleRepository.existsByUserIdAndRoleName(created.getId(), "STUDENT")).isTrue();
    assertThat(verificationCodeRepository.count()).isZero();
    assertThat(refreshTokenRepository.count()).isEqualTo(1);

    mockMvc
        .perform(
            post("/public/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "channel":"EMAIL",
                      "identifier":"new.student@gmail.com",
                      "password":"Secret123!"
                    }
                    """))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/public/auth/google")
                .contentType(APPLICATION_JSON)
                .content("{\"credential\":\"new-google-token\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(created.getId().toString()));
    assertThat(userRepository.count()).isEqualTo(1);
    assertThat(refreshTokenRepository.count()).isEqualTo(2);
  }

  @Test
  void googleAuthenticationLinksExistingEmailWithoutCreatingDuplicate() throws Exception {
    User existing =
        user("Existing Name", "existing@gmail.com", null, "Secret123!", UserStatus.ACTIVE);
    addRole(existing, "STUDENT");
    when(googleTokenVerifier.verify("existing-google-token"))
        .thenReturn(
            new GoogleProfile(
                "google-subject-existing", "existing@gmail.com", "Google Profile Name"));

    mockMvc
        .perform(
            post("/public/auth/google")
                .contentType(APPLICATION_JSON)
                .content("{\"credential\":\"existing-google-token\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(existing.getId().toString()))
        .andExpect(jsonPath("$.fullName").value("Existing Name"))
        .andExpect(jsonPath("$.accessToken").isNotEmpty());

    assertThat(userRepository.count()).isEqualTo(1);
    User linked = userRepository.findById(existing.getId()).orElseThrow();
    assertThat(linked.getGoogleSubject()).isEqualTo("google-subject-existing");
    assertThat(linked.getEmailVerifiedAt()).isNotNull();
    assertThat(linked.getPasswordHash()).isNotNull();
    assertThat(verificationCodeRepository.count()).isZero();
  }

  @Test
  void googleAuthenticationRejectsDifferentSubjectForLinkedEmail() throws Exception {
    User existing = user("Linked User", "linked@gmail.com", null, "Secret123!", UserStatus.ACTIVE);
    existing.setGoogleSubject("original-google-subject");
    existing.setEmailVerifiedAt(Instant.now());
    userRepository.save(existing);
    addRole(existing, "STUDENT");
    when(googleTokenVerifier.verify("other-google-token"))
        .thenReturn(new GoogleProfile("other-google-subject", "linked@gmail.com", "Other"));

    mockMvc
        .perform(
            post("/public/auth/google")
                .contentType(APPLICATION_JSON)
                .content("{\"credential\":\"other-google-token\"}"))
        .andExpect(status().isUnauthorized());

    assertThat(userRepository.count()).isEqualTo(1);
    assertThat(refreshTokenRepository.count()).isZero();
  }

  @Test
  void googleAuthenticationRejectsNonGmailAddress() throws Exception {
    when(googleTokenVerifier.verify("workspace-google-token"))
        .thenReturn(new GoogleProfile("workspace-subject", "person@example.com", "Workspace User"));

    mockMvc
        .perform(
            post("/public/auth/google")
                .contentType(APPLICATION_JSON)
                .content("{\"credential\":\"workspace-google-token\"}"))
        .andExpect(status().isUnauthorized());

    assertThat(userRepository.count()).isZero();
    assertThat(refreshTokenRepository.count()).isZero();
  }

  @Test
  void googlePostCallbackRequiresMatchingDoubleSubmitCsrfToken() throws Exception {
    when(googleTokenVerifier.verify("callback-google-token"))
        .thenReturn(new GoogleProfile("callback-subject", "callback@gmail.com", "Callback User"));

    mockMvc
        .perform(
            post("/public/auth/google/callback")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .cookie(new jakarta.servlet.http.Cookie("g_csrf_token", "csrf-token"))
                .param("credential", "callback-google-token")
                .param("g_csrf_token", "csrf-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=")));
  }

  @Test
  void googlePostCallbackRejectsMismatchedCsrfToken() throws Exception {
    mockMvc
        .perform(
            post("/public/auth/google/callback")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .cookie(new jakarta.servlet.http.Cookie("g_csrf_token", "cookie-token"))
                .param("credential", "unused-google-token")
                .param("g_csrf_token", "request-token"))
        .andExpect(status().isBadRequest());

    assertThat(userRepository.count()).isZero();
    assertThat(refreshTokenRepository.count()).isZero();
  }

  @Test
  void googleAuthenticationRejectsSuspendedAndSubjectEmailMismatchWithoutMutation()
      throws Exception {
    User suspended =
        user("Suspended", "suspended@gmail.com", null, "Secret123!", UserStatus.SUSPENDED);
    suspended.setGoogleSubject("suspended-subject");
    userRepository.saveAndFlush(suspended);
    addRole(suspended, "STUDENT");
    when(googleTokenVerifier.verify("suspended-token"))
        .thenReturn(new GoogleProfile("suspended-subject", "suspended@gmail.com", "Suspended"));

    mockMvc
        .perform(
            post("/public/auth/google")
                .contentType(APPLICATION_JSON)
                .content("{\"credential\":\"suspended-token\"}"))
        .andExpect(status().isUnauthorized());

    suspended.setStatus(UserStatus.ACTIVE);
    userRepository.saveAndFlush(suspended);
    when(googleTokenVerifier.verify("wrong-email-token"))
        .thenReturn(new GoogleProfile("suspended-subject", "different@gmail.com", "Different"));
    mockMvc
        .perform(
            post("/public/auth/google")
                .contentType(APPLICATION_JSON)
                .content("{\"credential\":\"wrong-email-token\"}"))
        .andExpect(status().isUnauthorized());

    assertThat(refreshTokenRepository.count()).isZero();
    assertThat(userRepository.count()).isEqualTo(1);
  }

  @Test
  void hybridAccountStillRequiresPasswordForEmailLoginAndRetainsPasswordAfterGoogleLink()
      throws Exception {
    User existing = user("Hybrid", "hybrid@gmail.com", null, "Secret123!", UserStatus.ACTIVE);
    existing.setEmailVerifiedAt(Instant.now());
    userRepository.saveAndFlush(existing);
    addRole(existing, "STUDENT");
    when(googleTokenVerifier.verify("hybrid-token"))
        .thenReturn(new GoogleProfile("hybrid-subject", "hybrid@gmail.com", "Ignored Name"));

    mockMvc
        .perform(
            post("/public/auth/google")
                .contentType(APPLICATION_JSON)
                .content("{\"credential\":\"hybrid-token\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/public/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"channel":"EMAIL","identifier":"hybrid@gmail.com","password":"Secret123!"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(existing.getId().toString()));

    mockMvc
        .perform(
            post("/public/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"channel\":\"EMAIL\",\"identifier\":\"hybrid@gmail.com\"}"))
        .andExpect(status().isBadRequest());
    assertThat(userRepository.findById(existing.getId()).orElseThrow().getPasswordHash())
        .isNotNull();
  }

  @Test
  void directRegistrationRequiresPasswordAndDatabaseRejectsUserWithNoAuthenticationMethod()
      throws Exception {
    mockMvc
        .perform(
            post("/public/auth/register")
                .contentType(APPLICATION_JSON)
                .content("{\"fullName\":\"No Password\",\"email\":\"no-password@gmail.com\"}"))
        .andExpect(status().isBadRequest());

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO users (email, full_name, status) VALUES (?, ?, 'ACTIVE')",
                    "constraint@gmail.com",
                    "Constraint"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO users (email, full_name, status, google_subject)"
                        + " VALUES (?, ?, 'ACTIVE', '')",
                    "blank-subject@gmail.com",
                    "Blank Subject"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(userRepository.count()).isZero();
  }

  @Test
  void googleCallbackRejectsMissingCsrfCookieBeforeCredentialVerification() throws Exception {
    mockMvc
        .perform(
            post("/public/auth/google/callback")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .param("credential", "unused-token")
                .param("g_csrf_token", "request-token"))
        .andExpect(status().isBadRequest());

    assertThat(userRepository.count()).isZero();
    assertThat(refreshTokenRepository.count()).isZero();
  }

  @Test
  void simultaneousFirstGoogleLoginsForSameEmailAreIdempotent() throws Exception {
    when(googleTokenVerifier.verify("concurrent-google-token"))
        .thenReturn(
            new GoogleProfile(
                "concurrent-google-subject", "concurrent@gmail.com", "Concurrent User"));
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var request =
          (java.util.concurrent.Callable<Integer>)
              () -> {
                ready.countDown();
                start.await();
                return mockMvc
                    .perform(
                        post("/public/auth/google")
                            .contentType(APPLICATION_JSON)
                            .content("{\"credential\":\"concurrent-google-token\"}"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              };
      var first = executor.submit(request);
      var second = executor.submit(request);
      ready.await();
      start.countDown();

      assertThat(first.get()).isEqualTo(200);
      assertThat(second.get()).isEqualTo(200);
    } finally {
      executor.shutdownNow();
    }

    assertThat(userRepository.findByEmail("concurrent@gmail.com")).isPresent();
    assertThat(userRepository.count()).isEqualTo(1);
    assertThat(refreshTokenRepository.count()).isEqualTo(2);
  }

  @Test
  void googleRefreshCookieIsScopedForLogoutAndLogoutRevokesServerSession() throws Exception {
    when(googleTokenVerifier.verify("logout-google-token"))
        .thenReturn(new GoogleProfile("logout-subject", "logout@gmail.com", "Logout User"));

    String setCookie =
        mockMvc
            .perform(
                post("/public/auth/google")
                    .contentType(APPLICATION_JSON)
                    .content("{\"credential\":\"logout-google-token\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader("Set-Cookie");
    assertThat(setCookie).contains("Path=/public/auth").contains("SameSite=Lax");
    String refreshToken = setCookie.substring("refresh_token=".length(), setCookie.indexOf(';'));

    mockMvc
        .perform(
            post("/public/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken)))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

    assertThat(refreshTokenRepository.findAll()).allMatch(token -> token.getRevokedAt() != null);
  }
}
