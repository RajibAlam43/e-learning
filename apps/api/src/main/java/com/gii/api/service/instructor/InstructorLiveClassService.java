package com.gii.api.service.instructor;

import com.gii.api.model.request.instructor.CreateLiveClassRequest;
import com.gii.api.model.request.instructor.UpdateLiveClassRequest;
import com.gii.api.model.response.instructor.InstructorLiveClassResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassStartResponse;
import com.gii.api.model.response.instructor.LiveClassRegistrantSummaryResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.live.LiveMeetingCreateRequest;
import com.gii.api.service.live.LiveMeetingCreateResult;
import com.gii.api.service.live.LiveMeetingProvisioningService;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassAttendance;
import com.gii.common.entity.live.LiveClassRegistrant;
import com.gii.common.entity.user.User;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.live.LiveClassAttendanceRepository;
import com.gii.common.repository.live.LiveClassRegistrantRepository;
import com.gii.common.repository.live.LiveClassRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class InstructorLiveClassService {
  private static final Duration CREATE_LEAD_TIME = Duration.ofMinutes(2);
  private static final int MAX_CAPACITY_LIMIT = 1000;
  private static final String DISPLAY_TIMEZONE = "Asia/Dhaka";

  private final CurrentUserService currentUserService;
  private final CourseSectionRepository courseSectionRepository;
  private final LiveClassRepository liveClassRepository;
  private final LiveClassRegistrantRepository liveClassRegistrantRepository;
  private final LiveClassAttendanceRepository liveClassAttendanceRepository;
  private final LiveMeetingProvisioningService liveMeetingProvisioningService;

  public InstructorLiveClassResponse create(
      UUID courseId, CreateLiveClassRequest request, Authentication authentication) {
    User instructor = currentUserService.getCurrentUser(authentication);
    CourseSection section =
        courseSectionRepository
            .findAssignedSectionForInstructor(courseId, request.sectionId(), instructor.getId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Not assigned to this course section"));
    validateSupportedProvider(request.provider());
    validateSchedule(request.startsAt(), request.endsAt());
    validateCapacity(request.maxCapacity());
    ensureNoProviderOverlap(request.provider(), request.startsAt(), request.endsAt());

    LiveMeetingCreateResult meeting =
        liveMeetingProvisioningService.createMeeting(
            LiveMeetingCreateRequest.builder()
                .provider(request.provider())
                .title(request.title().trim())
                .description(request.description())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .maxCapacity(request.maxCapacity())
                .build());
    LiveClass liveClass =
        LiveClass.builder()
            .course(section.getCourse())
            .section(section)
            .instructor(instructor)
            .title(request.title().trim())
            .description(request.description())
            .provider(request.provider())
            .providerMeetingId(meeting.meetingId())
            .hostStartUrl(meeting.hostStartUrl())
            .participantJoinUrl(meeting.participantJoinUrl())
            .startsAt(request.startsAt())
            .endsAt(request.endsAt())
            .maxCapacity(request.maxCapacity())
            .status(LiveClassStatus.SCHEDULED)
            .createdBy(instructor)
            .build();

    LiveClass saved = liveClassRepository.save(liveClass);
    return toLiveClassResponse(saved);
  }

  public InstructorLiveClassStartResponse start(UUID liveClassId, Authentication authentication) {
    UUID instructorId = currentUserService.getCurrentUserId(authentication);
    LiveClass liveClass = requireOwnedLiveClass(liveClassId, instructorId);

    if (liveClass.getStatus() == LiveClassStatus.CANCELLED
        || liveClass.getStatus() == LiveClassStatus.COMPLETED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot start class in current status");
    }

    liveClass.setStatus(LiveClassStatus.LIVE);
    liveClassRepository.save(liveClass);

    int approvedStudents =
        (int)
            liveClassRegistrantRepository.countByLiveClassIdAndStatus(
                liveClassId, LiveClassRegistrantStatus.APPROVED);
    int pendingStudents =
        (int)
            liveClassRegistrantRepository.countByLiveClassIdAndStatus(
                liveClassId, LiveClassRegistrantStatus.PENDING);

    return InstructorLiveClassStartResponse.builder()
        .liveClassId(liveClass.getId())
        .title(liveClass.getTitle())
        .provider(liveClass.getProvider())
        .hostStartUrl(liveClass.effectiveHostStartUrl())
        .meetingId(liveClass.effectiveMeetingId())
        .startsAt(liveClass.getStartsAt())
        .endsAt(liveClass.getEndsAt())
        .durationMinutes(
            Duration.between(liveClass.getStartsAt(), liveClass.getEndsAt()).toMinutes())
        .status(liveClass.getStatus())
        .registeredStudents(approvedStudents + pendingStudents)
        .approvedStudents(approvedStudents)
        .waitlistedStudents(0)
        .recordingEnabled(false)
        .recordingPlaybackUrl(null)
        .supportUrl(null)
        .helpEmail("support@gii.com")
        .build();
  }

  public InstructorLiveClassResponse update(
      UUID liveClassId, UpdateLiveClassRequest request, Authentication authentication) {
    UUID instructorId = currentUserService.getCurrentUserId(authentication);
    LiveClass liveClass = requireOwnedLiveClass(liveClassId, instructorId);

    if (request.title() != null && !request.title().isBlank()) {
      liveClass.setTitle(request.title().trim());
    }
    if (request.description() != null) {
      liveClass.setDescription(request.description());
    }

    Instant startsAt = request.startsAt() != null ? request.startsAt() : liveClass.getStartsAt();
    Instant endsAt = request.endsAt() != null ? request.endsAt() : liveClass.getEndsAt();
    validateSchedule(startsAt, endsAt);
    liveClass.setStartsAt(startsAt);
    liveClass.setEndsAt(endsAt);

    if (request.status() != null) {
      liveClass.setStatus(request.status());
    }

    LiveClass updated = liveClassRepository.save(liveClass);
    return toLiveClassResponse(updated);
  }

  public void delete(UUID liveClassId, Authentication authentication) {
    UUID instructorId = currentUserService.getCurrentUserId(authentication);
    LiveClass liveClass = requireOwnedLiveClass(liveClassId, instructorId);

    if (liveClass.getStatus() == LiveClassStatus.LIVE
        || liveClass.getStatus() == LiveClassStatus.COMPLETED) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Cannot delete started/completed class");
    }

    // Soft-delete by cancellation keeps audit history and registrants intact.
    liveClass.setStatus(LiveClassStatus.CANCELLED);
    liveClassRepository.save(liveClass);
  }

  private LiveClass requireOwnedLiveClass(UUID liveClassId, UUID instructorId) {
    return liveClassRepository
        .findByIdAndInstructorId(liveClassId, instructorId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Live class not found"));
  }

  private void validateSchedule(Instant startsAt, Instant endsAt) {
    if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid schedule");
    }
    if (startsAt.isBefore(Instant.now().plus(CREATE_LEAD_TIME))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Start time must be at least 2 minutes in the future");
    }
  }

  private void validateSupportedProvider(LiveClassProvider provider) {
    if (provider != LiveClassProvider.ZOOM && provider != LiveClassProvider.GOOGLE_MEET) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Provider must be ZOOM or GOOGLE_MEET");
    }
  }

  private void validateCapacity(Integer maxCapacity) {
    if (maxCapacity == null || maxCapacity < 1 || maxCapacity > MAX_CAPACITY_LIMIT) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Max capacity must be between 1 and 1000");
    }
  }

  private void ensureNoProviderOverlap(LiveClassProvider provider, Instant startsAt, Instant endsAt) {
    boolean overlap =
        liveClassRepository.existsOverlappingByProvider(
            provider,
            List.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE),
            startsAt,
            endsAt);
    if (overlap) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Provider host account already has overlapping live class");
    }
  }

  private InstructorLiveClassResponse toLiveClassResponse(LiveClass liveClass) {
    List<LiveClassRegistrant> registrants =
        liveClassRegistrantRepository.findByLiveClassIdOrderByCreatedAtAsc(liveClass.getId());
    List<LiveClassAttendance> attendanceRows =
        liveClassAttendanceRepository.findByLiveClassId(liveClass.getId());
    Map<UUID, LiveClassAttendance> attendanceByUserId = mapAttendanceByUserId(attendanceRows);

    List<LiveClassRegistrantSummaryResponse> registrantSummaries =
        registrants.stream()
            .map(
                registrant -> {
                  LiveClassAttendance attendance =
                      attendanceByUserId.get(registrant.getUser().getId());

                  return LiveClassRegistrantSummaryResponse.builder()
                      .registrantId(registrant.getId())
                      .userId(registrant.getUser().getId())
                      .studentName(registrant.getUser().getFullName())
                      .studentEmail(registrant.getUser().getEmail())
                      .status(registrant.getStatus())
                      .providerRegistrantId(registrant.getProviderRegistrantId())
                      .attended(attendance != null)
                      .joinedAt(attendance != null ? attendance.getJoinedAt() : null)
                      .leftAt(attendance != null ? attendance.getLeftAt() : null)
                      .durationSeconds(attendance != null ? attendance.getDurationSec() : null)
                      .registeredAt(registrant.getCreatedAt())
                      .build();
                })
            .toList();

    int attendedStudents = (int) attendanceRows.stream().filter(a -> a.getUser() != null).count();

    return InstructorLiveClassResponse.builder()
        .liveClassId(liveClass.getId())
        .title(liveClass.getTitle())
        .description(liveClass.getDescription())
        .courseId(liveClass.getCourse().getId())
        .courseName(liveClass.getCourse().getTitle())
        .sectionId(liveClass.getSection().getId())
        .sectionTitle(liveClass.getSection().getTitle())
        .instructorName(
            liveClass.getInstructor() != null ? liveClass.getInstructor().getFullName() : null)
        .instructorEmail(
            liveClass.getInstructor() != null ? liveClass.getInstructor().getEmail() : null)
        .startsAt(liveClass.getStartsAt())
        .endsAt(liveClass.getEndsAt())
        .durationMinutes(
            Duration.between(liveClass.getStartsAt(), liveClass.getEndsAt()).toMinutes())
        .timezone(DISPLAY_TIMEZONE)
        .status(liveClass.getStatus())
        .provider(liveClass.getProvider())
        .meetingId(liveClass.effectiveMeetingId())
        .hostStartUrl(liveClass.effectiveHostStartUrl())
        .joinUrl(liveClass.effectiveParticipantJoinUrl())
        .registeredStudents(registrantSummaries.size())
        .attendedStudents(attendedStudents)
        .registrants(registrantSummaries)
        .hasRecording(false)
        .recordingUrl(null)
        .recordingAvailableAt(null)
        .createdAt(liveClass.getCreatedAt())
        .updatedAt(liveClass.getUpdatedAt())
        .build();
  }

  private Map<UUID, LiveClassAttendance> mapAttendanceByUserId(List<LiveClassAttendance> rows) {
    Map<UUID, LiveClassAttendance> result = new HashMap<>();
    for (LiveClassAttendance row : rows) {
      if (row.getUser() != null) {
        result.putIfAbsent(row.getUser().getId(), row);
      }
    }
    return result;
  }

}
