package com.gii.api.service.live;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GoogleMeetCalendarService {
  private final WebClient.Builder clients;

  @Value("${integrations.google-calendar.base-url:https://www.googleapis.com/calendar/v3}")
  private String baseUrl;

  public LiveMeetingCreateResult createTeacherMeeting(
      LiveMeetingCreateRequest request, GoogleMeetCredentialsService.Grant grant) {
    String id = "lms" + UUID.randomUUID().toString().replace("-", "");
    WebClient client = client(grant);
    Map<String, Object> event = event(request, grant.email(), null);
    event.put("id", id);
    event.put(
        "conferenceData",
        Map.of(
            "createRequest",
            Map.of("requestId", id, "conferenceSolutionKey", Map.of("type", "hangoutsMeet"))));
    try {
      CalendarEvent result =
          client
              .post()
              .uri(uri ->
                  uri.path("/calendars/primary/events")
                      .queryParam("conferenceDataVersion", 1)
                      .queryParam("sendUpdates", "all")
                      .build())
              .bodyValue(event)
              .retrieve()
              .bodyToMono(CalendarEvent.class)
              .block(Duration.ofSeconds(25));
      for (int attempt = 0;
          (result == null || result.hangoutLink() == null) && attempt < 5;
          attempt++) {
        Thread.sleep(100L << attempt);
        result =
            client
                .get()
                .uri("/calendars/primary/events/" + id)
                .retrieve()
                .bodyToMono(CalendarEvent.class)
                .block(Duration.ofSeconds(5));
      }
      if (result == null
          || result.hangoutLink() == null
          || !result
              .hangoutLink()
              .matches("https://meet\\.google\\.com/[a-z]{3}-[a-z]{4}-[a-z]{3}")) {
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "Google has not finished creating the meeting. Try scheduling again.");
      }
      Map<String, Object> metadata = grant.metadata();
      metadata.put("googleCalendarEventId", id);
      return LiveMeetingCreateResult.builder()
          .meetingId("calendar/" + id)
          .hostStartUrl(result.hangoutLink())
          .participantJoinUrl(result.hangoutLink())
          .metadata(metadata)
          .build();
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Google meeting creation interrupted");
    } catch (RuntimeException failure) {
      try {
        delete(grant, id);
      } catch (RuntimeException cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }

  public Map<String, Object> synchronize(
      LiveMeetingCreateRequest request,
      GoogleMeetCredentialsService.Grant grant,
      String meetingUri,
      Map<String, Object> previous) {
    Map<String, Object> metadata = new HashMap<>(previous);
    String id = (String) metadata.get("googleCalendarEventId");
    if (id == null) {
      id = "lms" + UUID.randomUUID().toString().replace("-", "");
      Map<String, Object> event = event(request, grant.email(), meetingUri);
      event.put("id", id);
      client(grant)
          .post()
          .uri(
              uri -> uri.path("/calendars/primary/events").queryParam("sendUpdates", "all").build())
          .bodyValue(event)
          .retrieve()
          .toBodilessEntity()
          .block(Duration.ofSeconds(25));
      metadata.put("googleCalendarEventId", id);
    } else {
      update(request, grant, id, meetingUri);
    }
    return metadata;
  }

  public void update(
      LiveMeetingCreateRequest request,
      GoogleMeetCredentialsService.Grant grant,
      String id,
      String meetingUri) {
    client(grant)
        .patch()
        .uri(uri ->
            uri.path("/calendars/primary/events/" + id).queryParam("sendUpdates", "all").build())
        .bodyValue(event(request, grant.email(), meetingUri))
        .retrieve()
        .toBodilessEntity()
        .block(Duration.ofSeconds(25));
  }

  public void delete(GoogleMeetCredentialsService.Grant grant, String id) {
    client(grant)
        .delete()
        .uri(uri ->
            uri.path("/calendars/primary/events/" + id).queryParam("sendUpdates", "all").build())
        .retrieve()
        .onStatus(
            status -> status.value() == 404 || status.value() == 410,
            response -> reactor.core.publisher.Mono.empty())
        .toBodilessEntity()
        .block(Duration.ofSeconds(25));
  }

  private Map<String, Object> event(
      LiveMeetingCreateRequest request, String ownerEmail, String meetingUri) {
    Map<String, Object> event = new HashMap<>();
    event.put("summary", request.title() == null ? "LMS class" : request.title());
    event.put(
        "description",
        (request.description() == null ? "" : request.description())
            + (meetingUri == null ? "" : "\nJoin class: " + meetingUri));
    if (meetingUri != null) {
      event.put("location", meetingUri);
    }
    event.put(
        "start",
        Map.of(
            "dateTime",
            DateTimeFormatter.ISO_INSTANT.format(request.startsAt()),
            "timeZone",
            "UTC"));
    event.put(
        "end",
        Map.of(
            "dateTime", DateTimeFormatter.ISO_INSTANT.format(request.endsAt()), "timeZone", "UTC"));
    List<String> emails =
        request.calendarGuestEmails() == null ? List.of() : request.calendarGuestEmails();
    event.put(
        "attendees",
        emails.stream()
            .filter(email -> !email.equalsIgnoreCase(ownerEmail))
            .distinct()
            .map(email -> Map.of("email", email))
            .toList());
    return event;
  }

  private WebClient client(GoogleMeetCredentialsService.Grant grant) {
    return clients
        .clone()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + grant.accessToken())
        .defaultStatusHandler(
            status -> status.isError(),
            response ->
                response
                    .releaseBody()
                    .then(
                        reactor.core.publisher.Mono.error(
                            new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY,
                                failureMessage(response.statusCode().value())))))
        .build();
  }

  private static String failureMessage(int status) {
    return switch (status) {
      case 400 ->
          "Google Calendar rejected the event details (HTTP 400)."
              + " Check the class times and whether the owner's calendar supports Google Meet.";
      case 401 ->
          "Google Calendar authorization expired or is invalid (HTTP 401)."
              + " Reconnect the owner with Calendar permission.";
      case 403 ->
          "Google Calendar denied the request (HTTP 403)."
              + " Check Calendar permission, API enablement and account restrictions.";
      case 429 -> "Google Calendar rate limit reached (HTTP 429). Try again later.";
      default -> "Google Calendar request failed (HTTP " + status + "). Try again later.";
    };
  }

  private record CalendarEvent(String id, String hangoutLink) {}
}
