package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentLiveClassJoinResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassRegistrant;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.live.LiveClassRegistrantRepository;
import com.gii.common.repository.live.LiveClassRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentJoinLiveClassesService {

  private final CurrentUserService currentUserService;
  private final LiveClassRepository liveClassRepository;
  private final LiveClassRegistrantRepository registrantRepository;
  private final EnrollmentRepository enrollmentRepository;

  public StudentLiveClassJoinResponse execute(UUID liveClassId, Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    LiveClass liveClass =
        liveClassRepository
            .findById(liveClassId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Live class not found"));

    Enrollment enrollment =
        enrollmentRepository
            .findByUserIdAndCourseIdAndStatus(
                userId, liveClass.getCourse().getId(), EnrollmentStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Not enrolled in this course"));

    if (enrollment.getExpiresAt() != null && enrollment.getExpiresAt().isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment expired");
    }

    LiveClassRegistrant registrant =
        registrantRepository
            .findByLiveClassIdAndUserId(liveClassId, userId)
            .map(this::ensureJoinableRegistrantStatus)
            .orElseGet(() -> createApprovedRegistrant(liveClass, enrollment.getUser()));

    Instant now = Instant.now();
    boolean withinJoinWindow =
        !now.isBefore(liveClass.getStartsAt().minusSeconds(300))
            && now.isBefore(liveClass.getEndsAt());

    if (liveClass.getStatus() != LiveClassStatus.LIVE || !withinJoinWindow) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Class not joinable");
    }

    String joinUrl =
        registrant.getParticipantJoinUrl() != null
            ? registrant.getParticipantJoinUrl()
            : liveClass.effectiveParticipantJoinUrl();
    if (joinUrl == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Join link is unavailable");
    }

    return StudentLiveClassJoinResponse.builder()
        .liveClassId(liveClass.getId())
        .title(liveClass.getTitle())
        .status(liveClass.getStatus())
        .provider(liveClass.getProvider())
        .startsAt(liveClass.getStartsAt())
        .endsAt(liveClass.getEndsAt())
        .joinUrl(joinUrl)
        .meetingId(liveClass.effectiveMeetingId())
        .instructorName(
            liveClass.getInstructor() != null ? liveClass.getInstructor().getFullName() : null)
        .instructorEmail(
            liveClass.getInstructor() != null ? liveClass.getInstructor().getEmail() : null)
        .isRegistered(true)
        .participantEmail(enrollment.getUser().getEmail())
        .providerRegistrantId(registrant.getProviderRegistrantId())
        .supportEmail("support@gii.com")
        .recordingAvailable(false)
        .recordingUrl(null)
        .build();
  }

  private LiveClassRegistrant ensureJoinableRegistrantStatus(LiveClassRegistrant registrant) {
    if (registrant.getStatus() == LiveClassRegistrantStatus.CANCELLED
        || registrant.getStatus() == LiveClassRegistrantStatus.FAILED) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration is not joinable");
    }
    if (registrant.getStatus() == LiveClassRegistrantStatus.PENDING) {
      registrant.setStatus(LiveClassRegistrantStatus.APPROVED);
      return registrantRepository.save(registrant);
    }
    return registrant;
  }

  private LiveClassRegistrant createApprovedRegistrant(LiveClass liveClass, com.gii.common.entity.user.User user) {
    enforceCapacityForNewRegistrant(liveClass);
    LiveClassRegistrant registrant =
        LiveClassRegistrant.builder()
            .liveClass(liveClass)
            .user(user)
            .status(LiveClassRegistrantStatus.APPROVED)
            .participantJoinUrl(liveClass.effectiveParticipantJoinUrl())
            .build();
    return registrantRepository.save(registrant);
  }

  private void enforceCapacityForNewRegistrant(LiveClass liveClass) {
    Integer maxCapacity = liveClass.getMaxCapacity();
    if (maxCapacity == null) {
      return;
    }
    long approvedCount =
        registrantRepository.countByLiveClassIdAndStatus(
            liveClass.getId(), LiveClassRegistrantStatus.APPROVED);
    if (approvedCount >= maxCapacity) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Live class is full");
    }
  }
}
