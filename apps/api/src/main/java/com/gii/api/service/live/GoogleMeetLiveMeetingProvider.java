package com.gii.api.service.live;

import com.gii.common.enums.LiveClassProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class GoogleMeetLiveMeetingProvider implements LiveMeetingProvider {
  private static final Duration TIMEOUT = Duration.ofSeconds(30);
  public static final String MANAGED_HOSTS = "googleManagedCoHosts";
  private final WebClient.Builder webClientBuilder;
  private final GoogleMeetCredentialsService credentials;
  private final GoogleMeetCalendarService calendar;

  @Value("${integrations.google-meet.base-url:https://meet.googleapis.com/v2}")
  private String baseUrl;

  @Override
  public LiveClassProvider provider() {
    return LiveClassProvider.GOOGLE_MEET;
  }

  @Override
  public LiveMeetingCreateResult create(LiveMeetingCreateRequest request) {
    GoogleMeetCredentialsService.Grant grant = credentials.forCreation(request.primaryTeacherId());
    if ("CALENDAR".equals(grant.apiKind())) {
      return calendar.createTeacherMeeting(request, grant);
    }
    List<String> emails =
        "SHARED".equals(grant.mode()) ? normalizeEmails(request.coHostEmails()) : List.of();
    if (emails.isEmpty() && "SHARED".equals(grant.mode())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Google Meet requires a teacher email");
    }
    WebClient client = client(grant);
    Space space =
        client
            .post()
            .uri("/spaces")
            .bodyValue(
                "SHARED".equals(grant.mode())
                    ? Map.of(
                        "config",
                        Map.of(
                            "accessType",
                            "RESTRICTED",
                            "moderation",
                            "ON",
                            "entryPointAccess",
                            "ALL"))
                    : Map.of())
            .retrieve()
            .bodyToMono(Space.class)
            .block(TIMEOUT);
    if (space == null
        || !isSpaceName(space.name())
        || space.meetingUri() == null
        || !space.meetingUri().matches("https://meet\\.google\\.com/[a-z]{3}-[a-z]{4}-[a-z]{3}")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Google Meet returned no meeting space");
    }
    Map<String, Object> metadata =
        "SHARED".equals(grant.mode())
            ? synchronize(client, space.name(), emails, grant.metadata())
            : grant.metadata();
    if (credentials.calendarEnabled()) {
      try {
        metadata = calendar.synchronize(request, grant, space.meetingUri(), metadata);
      } catch (RuntimeException failure) {
        try {
          cancel(
              LiveMeetingCancelRequest.builder()
                  .providerMeetingId(space.name())
                  .metadata(metadata)
                  .build());
        } catch (RuntimeException cleanup) {
          failure.addSuppressed(cleanup);
        }
        throw failure;
      }
    }
    return LiveMeetingCreateResult.builder()
        .meetingId(space.name())
        .hostStartUrl(space.meetingUri())
        .participantJoinUrl(space.meetingUri())
        .metadata(metadata)
        .build();
  }

  @Override
  public void update(LiveMeetingUpdateRequest request) {
    if (request.metadata() != null
        && request.metadata().get("googleCalendarEventId") instanceof String id) {
      calendar.update(
          LiveMeetingCreateRequest.builder()
              .title(request.title())
              .description(request.description())
              .startsAt(request.startsAt())
              .endsAt(request.endsAt())
              .calendarGuestEmails(request.calendarGuestEmails())
              .build(),
          credentials.forMeeting(request.metadata()),
          id,
          null);
    }
  }

  public Map<String, Object> synchronizeCoHosts(
      String name, List<String> emails, Map<String, Object> previousMetadata) {
    requireSpaceName(name);
    GoogleMeetCredentialsService.Grant grant = credentials.forMeeting(previousMetadata);
    if (!credentials.hasCoHosts(previousMetadata)) {
      return previousMetadata;
    }
    WebClient client = client(grant);
    client
        .patch()
        .uri(uri -> uri.path("/" + name).queryParam("updateMask", "config.moderation").build())
        .bodyValue(Map.of("config", Map.of("moderation", "ON")))
        .retrieve()
        .toBodilessEntity()
        .block(TIMEOUT);
    return synchronize(client, name, normalizeEmails(emails), previousMetadata);
  }

  @Override
  public void cancel(LiveMeetingCancelRequest request) {
    if (!isSpaceName(request.providerMeetingId())
        && (request.metadata() == null
            || !"CALENDAR".equals(request.metadata().get("googleApiKind")))) {
      return;
    }
    GoogleMeetCredentialsService.Grant grant = credentials.forMeeting(request.metadata());
    if ("CALENDAR".equals(grant.apiKind())) {
      calendar.delete(grant, request.metadata().get("googleCalendarEventId").toString());
      return;
    }
    if (!isSpaceName(request.providerMeetingId())) {
      // Unstamped historical Calendar IDs aren't Meet resource names.
      return;
    }
    WebClient client = client(grant);
    String name = request.providerMeetingId();
    if (credentials.hasCoHosts(request.metadata())) {
      client
          .patch()
          .uri(uri ->
              uri.path("/" + name)
                  .queryParam("updateMask", "config.accessType,config.moderation")
                  .build())
          .bodyValue(Map.of("config", Map.of("accessType", "RESTRICTED", "moderation", "ON")))
          .retrieve()
          .toBodilessEntity()
          .block(TIMEOUT);
      synchronize(client, name, List.of(), request.metadata());
    }
    client
        .post()
        .uri("/" + name + ":endActiveConference")
        .bodyValue(Map.of())
        .retrieve()
        .toBodilessEntity()
        .block(TIMEOUT);
    if (request.metadata() != null
        && request.metadata().get("googleCalendarEventId") instanceof String id) {
      calendar.delete(grant, id);
    }
    // Meet has no spaces.delete method. Cancellation revokes managed co-hosts and LMS access.
  }

  private Map<String, Object> synchronize(
      WebClient client, String name, List<String> desired, Map<String, Object> previousMetadata) {
    Map<String, Member> members = members(client, name);
    Map<String, String> managed = new LinkedHashMap<>();
    List<Runnable> undo = new ArrayList<>();
    try {
      for (String email : desired) {
        Member existing = members.get(email);
        Member host;
        if (existing == null) {
          host = createMember(client, name, email, "COHOST");
          Member created = host;
          undo.add(() -> deleteMember(client, created.name()));
        } else if (!"COHOST".equals(existing.role())) {
          host = patchMember(client, existing.name(), "COHOST");
          undo.add(() -> patchMember(client, existing.name(), existing.role()));
        } else {
          host = existing;
        }
        if (host == null
            || host.name() == null
            || !host.name().startsWith(name + "/members/")
            || !"COHOST".equals(host.role())) {
          throw new ResponseStatusException(
              HttpStatus.BAD_GATEWAY, "Google did not grant co-host access");
        }
        managed.put(email, host.name());
      }
      Object old = previousMetadata == null ? null : previousMetadata.get(MANAGED_HOSTS);
      if (old instanceof Map<?, ?> oldHosts) {
        for (Object email : oldHosts.keySet()) {
          Member removed = members.get(email.toString());
          if (!managed.containsKey(email.toString()) && removed != null) {
            deleteMember(client, removed.name());
            undo.add(() -> createMember(client, name, removed.email(), removed.role()));
          }
        }
      }
    } catch (RuntimeException failure) {
      Collections.reverse(undo);
      for (Runnable compensation : undo) {
        try {
          compensation.run();
        } catch (RuntimeException cleanup) {
          failure.addSuppressed(cleanup);
        }
      }
      throw failure;
    }
    Map<String, Object> result =
        previousMetadata == null ? new HashMap<>() : new HashMap<>(previousMetadata);
    result.put(MANAGED_HOSTS, managed);
    return result;
  }

  private Map<String, Member> members(WebClient client, String name) {
    Map<String, Member> result = new HashMap<>();
    String next = "";
    do {
      String pageToken = next;
      Members page =
          client
              .get()
              .uri(uri ->
                  uri.path("/" + name + "/members")
                      .queryParam("pageSize", 100)
                      .queryParam("pageToken", pageToken)
                      .build())
              .retrieve()
              .bodyToMono(Members.class)
              .block(TIMEOUT);
      if (page == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY, "Google returned no co-host list");
      }
      if (page.members() != null) {
        for (Member member : page.members()) {
          if (member.email() != null) {
            result.put(member.email().toLowerCase(Locale.ROOT), member);
          }
        }
      }
      next = page.nextPageToken();
    } while (next != null && !next.isBlank());
    return result;
  }

  private Member createMember(WebClient client, String name, String email, String role) {
    return client
        .post()
        .uri("/" + name + "/members")
        .bodyValue(Map.of("email", email, "role", role))
        .retrieve()
        .bodyToMono(Member.class)
        .block(TIMEOUT);
  }

  private Member patchMember(WebClient client, String name, String role) {
    return client
        .patch()
        .uri(uri -> uri.path("/" + name).queryParam("updateMask", "role").build())
        .bodyValue(Map.of("role", role == null ? "ROLE_UNSPECIFIED" : role))
        .retrieve()
        .bodyToMono(Member.class)
        .block(TIMEOUT);
  }

  private void deleteMember(WebClient client, String name) {
    client
        .delete()
        .uri("/" + name)
        .retrieve()
        .onStatus(status -> status.value() == 404, response -> reactor.core.publisher.Mono.empty())
        .toBodilessEntity()
        .block(TIMEOUT);
  }

  private List<String> normalizeEmails(List<String> emails) {
    if (emails == null) {
      return List.of();
    }
    List<String> normalized =
        emails.stream()
            .map(email -> email.trim().toLowerCase(Locale.ROOT))
            .distinct()
            .sorted()
            .toList();
    if (normalized.size() > 25) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Google Meet permits up to 25 co-hosts");
    }
    return normalized;
  }

  public Map<String, Object> synchronizeCalendar(
      LiveMeetingCreateRequest request, String meetingUri, Map<String, Object> metadata) {
    if (!credentials.calendarEnabled() && !metadata.containsKey("googleCalendarEventId")) {
      return metadata;
    }
    return calendar.synchronize(request, credentials.forMeeting(metadata), meetingUri, metadata);
  }

  private WebClient client(GoogleMeetCredentialsService.Grant grant) {
    return webClientBuilder
        .clone()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + grant.accessToken())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultStatusHandler(
            status -> status.isError(),
            response ->
                response
                    .createException()
                    .map(error ->
                        new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Google Meet request failed. Check organizer eligibility,"
                                + " Meet API access and OAuth permissions.",
                            sanitized(error))))
        .build();
  }

  private RuntimeException sanitized(WebClientResponseException error) {
    // Don't retain Google's response body or organizer authorization in application errors.
    return new IllegalStateException("Google Meet HTTP " + error.getStatusCode().value());
  }

  public static boolean isSpaceName(String name) {
    return name != null && name.matches("spaces/[A-Za-z0-9_-]+");
  }

  private void requireSpaceName(String name) {
    if (!isSpaceName(name)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Legacy Google class needs a new Meet space");
    }
  }

  private record Space(String name, String meetingUri) {}

  private record Member(String name, String email, String role) {}

  private record Members(List<Member> members, String nextPageToken) {}
}
