package com.gii.api.service.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.enums.LiveClassProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

class GoogleMeetLiveMeetingProviderTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final Map<String, Map<String, String>> members = new LinkedHashMap<>();
  private final List<String> calls = new ArrayList<>();
  private final List<JsonNode> bodies = new ArrayList<>();
  private HttpServer server;
  private GoogleMeetLiveMeetingProvider provider;
  private String rejectedEmail;
  private int memberSequence;

  @Test
  void promotesAnExistingMemberInsteadOfCreatingDuplicateMembership() {
    members.put(
        "teacher@example.com",
        Map.of(
            "name",
            "spaces/space1/members/existing",
            "email",
            "teacher@example.com",
            "role",
            "ROLE_UNSPECIFIED"));
    provider.synchronizeCoHosts("spaces/space1", List.of("teacher@example.com"), Map.of());
    assertThat(members).hasSize(1);
    assertThat(members.get("teacher@example.com")).containsEntry("role", "COHOST");
    assertThat(calls).contains("PATCH /v2/spaces/space1/members/existing");
    assertThat(calls).doesNotContain("POST /v2/spaces/space1/members");
  }

  @BeforeEach
  void setup() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", this::handle);
    server.start();
    var credentials = mock(GoogleMeetCredentialsService.class);
    when(credentials.forCreation(any()))
        .thenReturn(
            new GoogleMeetCredentialsService.Grant("access", "SHARED", null, null, null, "MEET"));
    when(credentials.forMeeting(any()))
        .thenReturn(
            new GoogleMeetCredentialsService.Grant("access", "SHARED", null, null, null, "MEET"));
    when(credentials.hasCoHosts(any())).thenReturn(true);
    provider =
        new GoogleMeetLiveMeetingProvider(
            WebClient.builder(), credentials, mock(GoogleMeetCalendarService.class));
    String origin = "http://127.0.0.1:" + server.getAddress().getPort();
    ReflectionTestUtils.setField(provider, "baseUrl", origin + "/v2");
  }

  @AfterEach
  void cleanup() {
    server.stop(0);
  }

  @Test
  void createsSpaceAndAllCourseCoHostsWithoutCalendar() {
    var result = provider.create(request(List.of("Teacher2@example.com", "teacher1@example.com")));
    assertThat(result.meetingId()).isEqualTo("spaces/space1");
    assertThat(result.hostStartUrl()).isEqualTo("https://meet.google.com/abc-defg-hij");
    assertThat(result.participantJoinUrl()).isEqualTo(result.hostStartUrl());
    assertThat(members.keySet()).containsExactly("teacher1@example.com", "teacher2@example.com");
    assertThat(members.values())
        .allSatisfy(member -> assertThat(member.get("role")).isEqualTo("COHOST"));
    assertThat(calls).noneMatch(call -> call.contains("calendar") || call.contains("events"));
    assertThat(bodies.get(0).path("config").path("moderation").asText()).isEqualTo("ON");
    assertThat(result.metadata()).containsKey(GoogleMeetLiveMeetingProvider.MANAGED_HOSTS);
  }

  @Test
  void refreshesTeachersAndPreservesExplicitManualCoHost() {
    var initial = provider.create(request(List.of("old@example.com", "admin@example.com")));
    var metadata = new LinkedHashMap<>(initial.metadata());
    metadata.put(LiveClassSchedulingService.MANUAL_HOSTS, List.of("admin@example.com"));
    var updated =
        provider.synchronizeCoHosts(
            "spaces/space1", List.of("new@example.com", "admin@example.com"), metadata);
    assertThat(members.keySet()).containsExactlyInAnyOrder("new@example.com", "admin@example.com");
    assertThat(updated)
        .containsEntry(LiveClassSchedulingService.MANUAL_HOSTS, List.of("admin@example.com"));
  }

  @Test
  void compensatesNewRolesIfGoogleRejectsOneTeacher() {
    rejectedEmail = "second@example.com";
    assertThatThrownBy(() -> provider.create(request(List.of("first@example.com", rejectedEmail))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("502");
    assertThat(members).isEmpty();
  }

  @Test
  void cancellationRevokesManagedHostsAndEndsCallWithoutDeletingSpace() {
    var result = provider.create(request(List.of("teacher@example.com")));
    members.put("unmanaged@example.com", member("unmanaged@example.com", "unmanaged"));
    provider.cancel(
        LiveMeetingCancelRequest.builder()
            .provider(LiveClassProvider.GOOGLE_MEET)
            .providerMeetingId(result.meetingId())
            .metadata(result.metadata())
            .build());
    assertThat(members.keySet()).containsExactly("unmanaged@example.com");
    assertThat(calls).contains("POST /v2/spaces/space1:endActiveConference");
    assertThat(calls).doesNotContain("DELETE /v2/spaces/space1");
  }

  @Test
  void tooManyHostsFailBeforeAnyGoogleRequest() {
    List<String> emails =
        IntStream.range(0, 26).mapToObj(i -> "teacher" + i + "@example.com").toList();
    assertThatThrownBy(() -> provider.create(request(emails)))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(calls).isEmpty();
  }

  @Test
  void legacyCalendarCancellationAndScheduleChangesMakeNoCalendarCalls() {
    provider.cancel(LiveMeetingCancelRequest.builder().providerMeetingId("old-event-id").build());
    provider.update(LiveMeetingUpdateRequest.builder().providerMeetingId("spaces/space1").build());
    assertThat(calls).isEmpty();
  }

  private LiveMeetingCreateRequest request(List<String> emails) {
    return LiveMeetingCreateRequest.builder()
        .provider(LiveClassProvider.GOOGLE_MEET)
        .title("Class")
        .startsAt(Instant.now())
        .endsAt(Instant.now().plusSeconds(3600))
        .coHostEmails(emails)
        .build();
  }

  private Map<String, String> member(String email, String id) {
    return Map.of("name", "spaces/space1/members/" + id, "email", email, "role", "COHOST");
  }

  private void handle(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    String method = exchange.getRequestMethod();
    calls.add(method + " " + path);
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    if (path.equals("/token")) {
      json(exchange, 200, Map.of("access_token", "access"));
      return;
    }
    assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer access");
    JsonNode parsed = body.isBlank() ? mapper.createObjectNode() : mapper.readTree(body);
    if (!body.isBlank()) {
      bodies.add(parsed);
    }
    if (path.equals("/v2/spaces") && method.equals("POST")) {
      json(
          exchange,
          200,
          Map.of("name", "spaces/space1", "meetingUri", "https://meet.google.com/abc-defg-hij"));
    } else if (path.equals("/v2/spaces/space1/members") && method.equals("GET")) {
      json(exchange, 200, Map.of("members", new ArrayList<>(members.values())));
    } else if (path.equals("/v2/spaces/space1/members") && method.equals("POST")) {
      String email = parsed.path("email").asText();
      if (email.equals(rejectedEmail)) {
        json(exchange, 403, Map.of("error", "not eligible"));
      } else {
        var member = member(email, "member-" + ++memberSequence);
        members.put(email, member);
        json(exchange, 200, member);
      }
    } else if (path.startsWith("/v2/spaces/space1/members/") && method.equals("DELETE")) {
      members.entrySet().removeIf(entry -> ("/v2/" + entry.getValue().get("name")).equals(path));
      json(exchange, 200, Map.of());
    } else if (path.startsWith("/v2/spaces/space1/members/") && method.equals("PATCH")) {
      var existing =
          members.entrySet().stream()
              .filter(entry -> ("/v2/" + entry.getValue().get("name")).equals(path))
              .findFirst()
              .orElseThrow();
      var updated = new LinkedHashMap<>(existing.getValue());
      updated.put("role", parsed.path("role").asText());
      members.put(existing.getKey(), updated);
      json(exchange, 200, updated);
    } else if (method.equals("PATCH") || path.endsWith(":endActiveConference")) {
      json(exchange, 200, Map.of());
    } else {
      json(exchange, 404, Map.of());
    }
  }

  private void json(HttpExchange exchange, int status, Object value) throws IOException {
    byte[] bytes = mapper.writeValueAsBytes(value);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    try (var output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }
}
