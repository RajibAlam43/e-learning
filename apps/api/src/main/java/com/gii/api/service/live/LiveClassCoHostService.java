package com.gii.api.service.live;

import com.gii.api.model.response.live.LiveClassCoHostResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassProvisioningMode;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.repository.live.LiveClassRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class LiveClassCoHostService {
  private final LiveClassRepository classes;
  private final LiveClassSchedulingService scheduling;
  private final CurrentUserService users;

  public LiveClassCoHostResponse add(
      UUID classId, String requestedEmail, Authentication authentication) {
    var liveClass =
        classes
            .findByIdForUpdate(classId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Live class not found"));
    if (liveClass.getProvider() != LiveClassProvider.GOOGLE_MEET
        || liveClass.getProvisioningMode() != LiveClassProvisioningMode.API_PROVISIONED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Co-host management requires an LMS-created Google Meet class");
    }
    if (liveClass.getStatus() != LiveClassStatus.SCHEDULED
        && liveClass.getStatus() != LiveClassStatus.LIVE) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Co-hosts can only be added to scheduled or live classes");
    }
    if (!scheduling.canManageCoHosts(liveClass)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "This teacher-owned meeting has no LMS-managed co-hosts."
              + " Shared organizer mode applies to new classes.");
    }
    String email =
        requestedEmail == null
            ? scheduling.teacherEmail(users.getCurrentUser(authentication))
            : requestedEmail.trim().toLowerCase(Locale.ROOT);
    if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Provide the co-host's Google account email");
    }
    var metadata =
        liveClass.getProviderMetadata() == null
            ? new HashMap<String, Object>()
            : new HashMap<>(liveClass.getProviderMetadata());
    List<String> manual = new ArrayList<>();
    if (metadata.get(LiveClassSchedulingService.MANUAL_HOSTS) instanceof List<?> existing) {
      existing.forEach(value -> manual.add(value.toString()));
    }
    if (!manual.contains(email)) {
      manual.add(email);
    }
    metadata.put(LiveClassSchedulingService.MANUAL_HOSTS, manual);
    liveClass.setProviderMetadata(metadata);
    scheduling.prepareGoogle(liveClass);
    classes.saveAndFlush(liveClass);
    return LiveClassCoHostResponse.builder()
        .liveClassId(classId)
        .email(email)
        .role("COHOST")
        .coHostEmails(scheduling.coHosts(liveClass))
        .joinUrl(liveClass.effectiveParticipantJoinUrl())
        .build();
  }
}
