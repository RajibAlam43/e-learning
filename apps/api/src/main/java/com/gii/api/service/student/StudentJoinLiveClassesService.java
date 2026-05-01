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
            .findByLiveClassIdAndUserIdAndStatus(
                liveClassId, userId, LiveClassRegistrantStatus.APPROVED)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Not registered for live class"));

    boolean liveOrCompleted =
        liveClass.getStatus() == LiveClassStatus.LIVE
            || liveClass.getStatus() == LiveClassStatus.COMPLETED;
    if (!liveOrCompleted && Instant.now().isBefore(liveClass.getStartsAt().minusSeconds(300))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Class not yet joinable");
    }

    String joinUrl =
        registrant.getZoomJoinUrl() != null
            ? registrant.getZoomJoinUrl()
            : liveClass.effectiveParticipantJoinUrl();
    if (joinUrl == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Join link is unavailable");
    }

    return StudentLiveClassJoinResponse.builder()
        .liveClassId(liveClass.getId())
        .title(liveClass.getTitle())
        .status(liveClass.getStatus())
        .startsAt(liveClass.getStartsAt())
        .endsAt(liveClass.getEndsAt())
        .zoomJoinUrl(joinUrl)
        .zoomMeetingId(liveClass.effectiveMeetingId())
        .instructorName(
            liveClass.getInstructor() != null ? liveClass.getInstructor().getFullName() : null)
        .instructorEmail(
            liveClass.getInstructor() != null ? liveClass.getInstructor().getEmail() : null)
        .isRegistered(true)
        .participantEmail(enrollment.getUser().getEmail())
        .zoomRegistrantId(registrant.getZoomRegistrantId())
        .supportEmail("support@gii.com")
        .recordingAvailable(false)
        .recordingUrl(null)
        .build();
  }
}
