package com.gii.api.service.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

class GoogleMeetCalendarServiceTest {
  @Test
  void primaryTeachersTokenCreatesCalendarConferenceAndInvitesOtherTeachers() throws Exception {
    var mapper = new ObjectMapper();
    var requestBody = new AtomicReference<JsonNode>();
    var authorization = new AtomicReference<String>();
    var query = new AtomicReference<String>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/calendars/primary/events",
        exchange -> {
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          query.set(exchange.getRequestURI().getQuery());
          requestBody.set(mapper.readTree(exchange.getRequestBody()));
          byte[] result =
              "{\"id\":\"event\",\"hangoutLink\":\"https://meet.google.com/abc-defg-hij\"}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, result.length);
          try (var output = exchange.getResponseBody()) {
            output.write(result);
          }
        });
    server.start();
    try {
      var calendar = new GoogleMeetCalendarService(WebClient.builder());
      ReflectionTestUtils.setField(
          calendar, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
      var grant =
          new GoogleMeetCredentialsService.Grant(
              "primary-teacher-access",
              "TEACHER",
              UUID.randomUUID(),
              "primary-subject",
              "primary@gmail.com",
              "CALENDAR");
      var result =
          calendar.createTeacherMeeting(
              LiveMeetingCreateRequest.builder()
                  .title("Class")
                  .startsAt(Instant.parse("2026-11-26T16:00:00Z"))
                  .endsAt(Instant.parse("2026-11-26T17:30:00Z"))
                  .calendarGuestEmails(List.of("primary@gmail.com", "assistant@gmail.com"))
                  .build(),
              grant);
      assertThat(authorization.get()).isEqualTo("Bearer primary-teacher-access");
      assertThat(query.get()).contains("conferenceDataVersion=1", "sendUpdates=all");
      assertThat(requestBody.get().path("start").path("dateTime").asText())
          .isEqualTo("2026-11-26T16:00:00Z");
      assertThat(requestBody.get().path("end").path("dateTime").asText())
          .isEqualTo("2026-11-26T17:30:00Z");
      assertThat(
              requestBody
                  .get()
                  .path("conferenceData")
                  .path("createRequest")
                  .path("conferenceSolutionKey")
                  .path("type")
                  .asText())
          .isEqualTo("hangoutsMeet");
      assertThat(requestBody.get().path("attendees")).hasSize(1);
      assertThat(requestBody.get().path("attendees").get(0).path("email").asText())
          .isEqualTo("assistant@gmail.com");
      assertThat(result.metadata())
          .containsEntry("googleOrganizerMode", "TEACHER")
          .containsEntry("googleOwnerSubject", "primary-subject")
          .doesNotContainKey("accessToken");
      assertThat(result.meetingId()).startsWith("calendar/");
      assertThat(result.hostStartUrl()).isEqualTo(result.participantJoinUrl());
    } finally {
      server.stop(0);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "400,event details",
    "401,Reconnect",
    "403,denied",
    "429,rate limit",
    "500,Try again"
  })
  void calendarFailuresIdentifyTheStatusWithoutExposingGoogleResponse(int status, String hint)
      throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/calendars/primary/events",
        exchange -> {
          exchange.getRequestBody().readAllBytes();
          byte[] body =
              "{\"error\":{\"message\":\"private@example.com secret-token\"}}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(
              exchange.getRequestMethod().equals("DELETE") ? 404 : status, body.length);
          try (var output = exchange.getResponseBody()) {
            output.write(body);
          }
        });
    server.start();
    try {
      var calendar = new GoogleMeetCalendarService(WebClient.builder());
      ReflectionTestUtils.setField(
          calendar, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
      var grant =
          new GoogleMeetCredentialsService.Grant(
              "secret-token",
              "TEACHER",
              UUID.randomUUID(),
              "subject",
              "private@example.com",
              "CALENDAR");
      var request =
          LiveMeetingCreateRequest.builder()
              .title("Class")
              .startsAt(Instant.parse("2026-11-26T16:00:00Z"))
              .endsAt(Instant.parse("2026-11-26T17:00:00Z"))
              .build();

      var error =
          catchThrowableOfType(
              ResponseStatusException.class, () -> calendar.createTeacherMeeting(request, grant));

      assertThat(error.getStatusCode().value()).isEqualTo(502);
      assertThat(error.getReason())
          .contains("HTTP " + status, hint)
          .doesNotContain("private@example.com", "secret-token");
      if (status != 401) {
        assertThat(error.getReason()).doesNotContain("Reconnect");
      }
    } finally {
      server.stop(0);
    }
  }
}
