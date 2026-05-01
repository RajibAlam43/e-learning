package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentLiveClassSummaryResponse;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentUpcomingLiveClasses {

  private final CurrentUserService currentUserService;
  private final EnrollmentRepository enrollmentRepository;
  private final LiveClassRepository liveClassRepository;
  private final LiveClassRegistrantRepository registrantRepository;

  public List<StudentLiveClassSummaryResponse> execute(Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    List<Enrollment> enrollments =
        enrollmentRepository.findByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE);
    if (enrollments.isEmpty()) {
      return List.of();
    }

    List<UUID> courseIds = enrollments.stream().map(e -> e.getCourse().getId()).toList();
    List<LiveClass> liveClasses =
        liveClassRepository.findUpcomingByCourseIds(
            courseIds, List.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE), Instant.now());

    Map<UUID, LiveClassRegistrant> registrantByClassId =
        registrantRepository
            .findByUserIdAndLiveClassIds(
                userId, liveClasses.stream().map(LiveClass::getId).toList())
            .stream()
            .collect(Collectors.toMap(r -> r.getLiveClass().getId(), r -> r));

    return liveClasses.stream()
        .map(liveClass -> toSummary(liveClass, registrantByClassId.get(liveClass.getId())))
        .toList();
  }

  StudentLiveClassSummaryResponse toSummary(LiveClass liveClass, LiveClassRegistrant registrant) {
    Instant now = Instant.now();
    boolean isLive =
        liveClass.getStatus() == LiveClassStatus.LIVE
            || (!now.isBefore(liveClass.getStartsAt()) && now.isBefore(liveClass.getEndsAt()));

    boolean isRegistered =
        registrant != null && registrant.getStatus() == LiveClassRegistrantStatus.APPROVED;
    String joinUrl =
        registrant != null && registrant.getZoomJoinUrl() != null
            ? registrant.getZoomJoinUrl()
            : liveClass.effectiveParticipantJoinUrl();
    boolean canJoin = isLive && isRegistered && joinUrl != null;

    return StudentLiveClassSummaryResponse.builder()
        .liveClassId(liveClass.getId())
        .title(liveClass.getTitle())
        .description(liveClass.getDescription())
        .instructorName(
            liveClass.getInstructor() != null ? liveClass.getInstructor().getFullName() : null)
        .instructorImageUrl(null)
        .startsAt(liveClass.getStartsAt())
        .endsAt(liveClass.getEndsAt())
        .durationMinutes(
            Duration.between(liveClass.getStartsAt(), liveClass.getEndsAt()).toMinutes())
        .timeZoneLabel(null)
        .courseId(liveClass.getCourse().getId())
        .courseName(liveClass.getCourse().getTitle())
        .lessonId(liveClass.getLesson().getId())
        .lessonTitle(liveClass.getLesson().getTitle())
        .status(liveClass.getStatus())
        .statusLabel(statusLabel(liveClass, isLive))
        .isLive(isLive)
        .isRegistered(isRegistered)
        .canJoin(canJoin)
        .joinUrl(canJoin ? joinUrl : null)
        .hasRecording(false)
        .recordingUrl(null)
        .build();
  }

  private String statusLabel(LiveClass liveClass, boolean isLive) {
    if (isLive) {
      return "Live Now";
    }
    return switch (liveClass.getStatus()) {
      case SCHEDULED -> "Upcoming";
      case COMPLETED -> "Completed";
      case CANCELLED -> "Cancelled";
      case FAILED -> "Failed";
      case LIVE -> "Live Now";
    };
  }
}
