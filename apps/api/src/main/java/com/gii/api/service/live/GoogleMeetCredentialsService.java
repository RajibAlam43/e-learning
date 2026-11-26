package com.gii.api.service.live;

import com.gii.api.service.live.oauth.GoogleMeetConnectionService;
import com.gii.api.service.live.oauth.GoogleMeetOauthClientService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GoogleMeetCredentialsService {
  private final GoogleMeetConnectionService connections;
  private final GoogleMeetOauthClientService oauth;

  @Value("${integrations.google-meet.shared-organizer-enabled:false}")
  private boolean shared;

  @Value("${integrations.google-meet.calendar-sync-enabled:true}")
  private boolean calendar;

  @Value("${integrations.google-oauth.refresh-token:}")
  private String sharedRefreshToken;

  public boolean sharedEnabled() {
    return shared;
  }

  public boolean calendarEnabled() {
    return calendar;
  }

  public Grant forCreation(UUID primaryTeacherId) {
    if (shared) {
      return sharedGrant();
    }
    if (primaryTeacherId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Assign a primary teacher before creating Google Meet classes");
    }
    var credential =
        connections.credential(
            primaryTeacherId,
            null,
            calendar
                ? GoogleMeetConnectionService.CALENDAR_SCOPE
                : GoogleMeetConnectionService.MEET_SCOPE);
    return new Grant(
        credential.accessToken(),
        "TEACHER",
        credential.userId(),
        credential.subject(),
        credential.email(),
        calendar ? "CALENDAR" : "MEET");
  }

  public Grant forMeeting(Map<String, Object> metadata) {
    // Unstamped historical spaces were created using the shared organizer.
    if (metadata == null || !"TEACHER".equals(metadata.get("googleOrganizerMode"))) {
      return sharedGrant();
    }
    String kind = metadata.getOrDefault("googleApiKind", "MEET").toString();
    var credential =
        connections.credential(
            UUID.fromString(metadata.get("googleOwnerUserId").toString()),
            metadata.get("googleOwnerSubject").toString(),
            "CALENDAR".equals(kind)
                ? GoogleMeetConnectionService.CALENDAR_SCOPE
                : GoogleMeetConnectionService.MEET_SCOPE);
    return new Grant(
        credential.accessToken(),
        "TEACHER",
        credential.userId(),
        credential.subject(),
        credential.email(),
        kind);
  }

  public boolean hasCoHosts(Map<String, Object> metadata) {
    return metadata == null || !"TEACHER".equals(metadata.get("googleOrganizerMode"));
  }

  private Grant sharedGrant() {
    if (sharedRefreshToken.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Configure shared Google organizer authorization");
    }
    return new Grant(oauth.refresh(sharedRefreshToken), "SHARED", null, null, null, "MEET");
  }

  @Builder
  public record Grant(
      @com.fasterxml.jackson.annotation.JsonIgnore String accessToken,
      String mode,
      UUID userId,
      String subject,
      String email,
      String apiKind) {
    public Map<String, Object> metadata() {
      Map<String, Object> result = new HashMap<>();
      result.put("googleOrganizerMode", mode);
      result.put("googleApiKind", apiKind);
      if (userId != null) {
        result.put("googleOwnerUserId", userId.toString());
        result.put("googleOwnerSubject", subject);
        result.put("googleOwnerEmail", email);
      }
      return result;
    }
  }
}
