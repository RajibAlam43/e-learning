package com.gii.api.service.instructor;

import com.gii.api.model.request.instructor.CreateLiveClassRequest;
import com.gii.api.model.request.instructor.UpdateLiveClassRequest;
import com.gii.api.model.response.instructor.InstructorLiveClassResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassStartResponse;
import com.gii.api.model.response.instructor.LiveClassRegistrantSummaryResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.live.LiveMeetingCancelRequest;
import com.gii.api.service.live.LiveMeetingCreateRequest;
import com.gii.api.service.live.LiveMeetingCreateResult;
import com.gii.api.service.live.LiveMeetingProvisioningService;
import com.gii.api.service.live.LiveMeetingUpdateRequest;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.progress.EnrollmentCompletionService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.SectionItem;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassAttendance;
import com.gii.common.entity.live.LiveClassRegistrant;
import com.gii.common.entity.live.LiveClassSlot;
import com.gii.common.entity.user.User;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassProvisioningMode;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.SectionItemType;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.SectionItemRepository;
import com.gii.common.repository.live.LiveClassAttendanceRepository;
import com.gii.common.repository.live.LiveClassRegistrantRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.live.LiveClassSlotRepository;
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
  private final CourseRepository courseRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final CourseSectionRepository courseSectionRepository;
  private final SectionItemRepository sectionItemRepository;
  private final LiveClassRepository liveClassRepository;
  private final LiveClassSlotRepository liveClassSlotRepository;
  private final LiveClassRegistrantRepository liveClassRegistrantRepository;
  private final LiveClassAttendanceRepository liveClassAttendanceRepository;
  private final LiveMeetingProvisioningService liveMeetingProvisioningService;
  private final LocalizedContentService localizedContentService;
  private final EnrollmentCompletionService enrollmentCompletionService;

  public InstructorLiveClassResponse create(
      UUID courseId, CreateLiveClassRequest request, Authentication authentication) {
    User instructor = currentUserService.getCurrentUser(authentication);
    if (request.liveClassItemId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "liveClassItemId is required for instructor scheduling");
    }
    if (request.position() != null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Instructors cannot reorder curriculum items");
    }
    LiveClassSlot slot =
        liveClassSlotRepository
            .findByIdForUpdate(request.liveClassItemId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Live class item not found"));
    final CourseSection section =
        courseSectionRepository
            .findAssignedSectionForInstructor(
                courseId, slot.getSection().getId(), instructor.getId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Not assigned to this course section"));
    final Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    if (liveClassRepository.existsByCourseIdAndSlotId(courseId, slot.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Live class item is already scheduled for this course");
    }
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
    try {
      LiveClass liveClass =
          LiveClass.builder()
              .course(course)
              .slot(slot)
              .titleOverride(request.title().trim())
              .titleEnOverride(request.titleEn())
              .descriptionOverride(request.description())
              .descriptionEnOverride(request.descriptionEn())
              .provider(request.provider())
              .providerMeetingId(meeting.meetingId())
              .hostStartUrl(meeting.hostStartUrl())
              .participantJoinUrl(meeting.participantJoinUrl())
              .provisioningMode(LiveClassProvisioningMode.API_PROVISIONED)
              .startsAt(request.startsAt())
              .endsAt(request.endsAt())
              .maxCapacity(request.maxCapacity())
              .status(LiveClassStatus.SCHEDULED)
              .build();

      LiveClass saved = liveClassRepository.saveAndFlush(liveClass);
      return toLiveClassResponse(saved);
    } catch (RuntimeException persistenceFailure) {
      compensateCreatedMeeting(request.provider(), meeting.meetingId(), persistenceFailure);
      throw persistenceFailure;
    }
  }

  public InstructorLiveClassStartResponse start(UUID liveClassId, Authentication authentication) {
    UUID instructorId = currentUserService.getCurrentUserId(authentication);
    LiveClass liveClass = requireOwnedLiveClass(liveClassId, instructorId);

    if (liveClass.getStatus() != LiveClassStatus.SCHEDULED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Only scheduled classes can be started");
    }
    if ((isApiProvisioned(liveClass) && isBlank(liveClass.effectiveMeetingId()))
        || isBlank(liveClass.effectiveParticipantJoinUrl())
        || isBlank(liveClass.effectiveHostStartUrl())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Live class meeting is not provisioned");
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
        .title(localizedContentService.text(liveClass.getTitle(), liveClass.getTitleEn()))
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
    boolean mutatingMetadata =
        request.title() != null
            || request.titleEn() != null
            || request.description() != null
            || request.descriptionEn() != null
            || request.startsAt() != null
            || request.endsAt() != null;
    if (mutatingMetadata && liveClass.getStatus() != LiveClassStatus.SCHEDULED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Only scheduled classes can be edited");
    }

    if (request.title() != null && !request.title().isBlank()) {
      liveClass.setTitle(request.title().trim());
    }
    if (request.titleEn() != null) {
      liveClass.setTitleEn(request.titleEn().trim());
    }
    if (request.description() != null) {
      liveClass.setDescription(request.description());
    }
    if (request.descriptionEn() != null) {
      liveClass.setDescriptionEn(request.descriptionEn());
    }

    if (request.startsAt() != null || request.endsAt() != null) {
      Instant startsAt = request.startsAt() != null ? request.startsAt() : liveClass.getStartsAt();
      Instant endsAt = request.endsAt() != null ? request.endsAt() : liveClass.getEndsAt();
      validateSchedule(startsAt, endsAt);
      if (isApiProvisioned(liveClass)) {
        ensureNoProviderOverlap(liveClass.getProvider(), startsAt, endsAt, liveClass.getId());
      }
      syncProviderUpdate(
          liveClass,
          request.title() != null && !request.title().isBlank()
              ? request.title().trim()
              : liveClass.getTitle(),
          request.description() != null ? request.description() : liveClass.getDescription(),
          startsAt,
          endsAt);
      liveClass.setStartsAt(startsAt);
      liveClass.setEndsAt(endsAt);
    }
    if (request.startsAt() == null
        && request.endsAt() == null
        && (request.title() != null || request.description() != null)) {
      syncProviderUpdate(
          liveClass,
          request.title() != null && !request.title().isBlank()
              ? request.title().trim()
              : liveClass.getTitle(),
          request.description() != null ? request.description() : liveClass.getDescription(),
          liveClass.getStartsAt(),
          liveClass.getEndsAt());
    }

    if (request.status() != null) {
      validateStatusTransitionForUpdate(liveClass.getStatus(), request.status());
      liveClass.setStatus(request.status());
      if (request.status() == LiveClassStatus.COMPLETED
          || request.status() == LiveClassStatus.CANCELLED
          || request.status() == LiveClassStatus.FAILED) {
        liveClassRepository.saveAndFlush(liveClass);
        enrollmentCompletionService.refreshCourse(liveClass.getCourse().getId());
      }
    }

    if (request.position() != null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Instructors cannot reorder curriculum items");
    }

    LiveClass updated = liveClassRepository.save(liveClass);
    return toLiveClassResponse(updated);
  }

  public InstructorLiveClassResponse cancel(UUID liveClassId, Authentication authentication) {
    UUID instructorId = currentUserService.getCurrentUserId(authentication);
    LiveClass liveClass = requireOwnedLiveClass(liveClassId, instructorId);

    if (liveClass.getStatus() != LiveClassStatus.SCHEDULED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Only scheduled classes can be cancelled");
    }

    // Cancel keeps audit history and registrants intact.
    syncProviderCancel(liveClass);
    liveClass.setStatus(LiveClassStatus.CANCELLED);
    LiveClass cancelled = liveClassRepository.saveAndFlush(liveClass);
    enrollmentCompletionService.refreshCourse(cancelled.getCourse().getId());
    return toLiveClassResponse(cancelled);
  }

  private void syncProviderUpdate(
      LiveClass liveClass, String title, String description, Instant startsAt, Instant endsAt) {
    if (!isApiProvisioned(liveClass)) {
      return;
    }
    if (isBlank(liveClass.effectiveMeetingId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Live class meeting is not provisioned");
    }
    liveMeetingProvisioningService.updateMeeting(
        LiveMeetingUpdateRequest.builder()
            .provider(liveClass.getProvider())
            .providerMeetingId(liveClass.effectiveMeetingId())
            .title(title)
            .description(description)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .build());
  }

  private void syncProviderCancel(LiveClass liveClass) {
    if (!isApiProvisioned(liveClass)) {
      return;
    }
    if (isBlank(liveClass.effectiveMeetingId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Live class meeting is not provisioned");
    }
    liveMeetingProvisioningService.cancelMeeting(
        LiveMeetingCancelRequest.builder()
            .provider(liveClass.getProvider())
            .providerMeetingId(liveClass.effectiveMeetingId())
            .build());
  }

  private LiveClass requireOwnedLiveClass(UUID liveClassId, UUID instructorId) {
    return liveClassRepository
        .findByIdAssignedToInstructor(liveClassId, instructorId)
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

  private void ensureNoProviderOverlap(
      LiveClassProvider provider, Instant startsAt, Instant endsAt) {
    liveClassRepository.acquireProviderSchedulingLock(provider.ordinal());
    boolean overlap =
        liveClassRepository.existsOverlappingByProvider(
            provider, List.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE), startsAt, endsAt);
    if (overlap) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Provider host account already has overlapping live class");
    }
  }

  private void ensureNoProviderOverlap(
      LiveClassProvider provider, Instant startsAt, Instant endsAt, UUID excludingLiveClassId) {
    liveClassRepository.acquireProviderSchedulingLock(provider.ordinal());
    boolean overlap =
        liveClassRepository.findAll().stream()
            .filter(
                lc ->
                    !lc.getId().equals(excludingLiveClassId)
                        && lc.getProvider() == provider
                        && (lc.getStatus() == LiveClassStatus.SCHEDULED
                            || lc.getStatus() == LiveClassStatus.LIVE))
            .anyMatch(lc -> lc.getStartsAt().isBefore(endsAt) && lc.getEndsAt().isAfter(startsAt));
    if (overlap) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Provider host account already has overlapping live class");
    }
  }

  private void compensateCreatedMeeting(
      LiveClassProvider provider, String meetingId, RuntimeException persistenceFailure) {
    try {
      liveMeetingProvisioningService.cancelMeeting(
          LiveMeetingCancelRequest.builder()
              .provider(provider)
              .providerMeetingId(meetingId)
              .build());
    } catch (RuntimeException compensationFailure) {
      persistenceFailure.addSuppressed(compensationFailure);
    }
  }

  private void validateStatusTransitionForUpdate(LiveClassStatus current, LiveClassStatus next) {
    if (current == next) {
      return;
    }
    boolean valid =
        switch (current) {
          case SCHEDULED -> next == LiveClassStatus.CANCELLED;
          case LIVE -> next == LiveClassStatus.COMPLETED;
          case COMPLETED, CANCELLED, FAILED -> false;
        };
    if (!valid) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid live class status transition");
    }
  }

  private InstructorLiveClassResponse toLiveClassResponse(LiveClass liveClass) {
    List<LiveClassRegistrant> registrants =
        liveClassRegistrantRepository.findByLiveClassIdOrderByCreatedAtAsc(liveClass.getId());
    List<LiveClassAttendance> attendanceRows =
        liveClassAttendanceRepository.findByLiveClassId(liveClass.getId());
    Map<UUID, AttendanceSummary> attendanceByUserId = mapAttendanceByUserId(attendanceRows);

    List<LiveClassRegistrantSummaryResponse> registrantSummaries =
        registrants.stream()
            .map(
                registrant -> {
                  AttendanceSummary attendance =
                      attendanceByUserId.get(registrant.getUser().getId());

                  return LiveClassRegistrantSummaryResponse.builder()
                      .registrantId(registrant.getId())
                      .userId(registrant.getUser().getId())
                      .studentName(registrant.getUser().getFullName())
                      .studentEmail(registrant.getUser().getEmail())
                      .status(registrant.getStatus())
                      .providerRegistrantId(registrant.getProviderRegistrantId())
                      .attended(attendance != null)
                      .joinedAt(attendance != null ? attendance.joinedAt() : null)
                      .leftAt(attendance != null ? attendance.leftAt() : null)
                      .durationSeconds(attendance != null ? attendance.durationSeconds() : null)
                      .registeredAt(registrant.getCreatedAt())
                      .build();
                })
            .toList();

    int attendedStudents = attendanceByUserId.size();

    return InstructorLiveClassResponse.builder()
        .liveClassId(liveClass.getId())
        .title(localizedContentService.text(liveClass.getTitle(), liveClass.getTitleEn()))
        .description(
            localizedContentService.text(liveClass.getDescription(), liveClass.getDescriptionEn()))
        .courseId(liveClass.getCourse().getId())
        .courseName(
            localizedContentService.text(
                liveClass.getCourse().getTitle(), liveClass.getCourse().getTitleEn()))
        .sectionId(liveClass.getSection().getId())
        .sectionTitle(
            localizedContentService.text(
                liveClass.getSection().getTitle(), liveClass.getSection().getTitleEn()))
        .position(positionOf(liveClass.getId()))
        .instructorName(primaryInstructorName(liveClass.getCourse()))
        .instructorEmail(primaryInstructorEmail(liveClass.getCourse()))
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

  private Map<UUID, AttendanceSummary> mapAttendanceByUserId(List<LiveClassAttendance> rows) {
    Map<UUID, AttendanceSummary> result = new HashMap<>();
    for (LiveClassAttendance row : rows) {
      if (row.getUser() != null && row.getJoinedAt() != null) {
        result.merge(
            row.getUser().getId(), AttendanceSummary.from(row), AttendanceSummary::combine);
      }
    }
    return result;
  }

  private record AttendanceSummary(Instant joinedAt, Instant leftAt, Integer durationSeconds) {

    private static AttendanceSummary from(LiveClassAttendance attendance) {
      return new AttendanceSummary(
          attendance.getJoinedAt(), attendance.getLeftAt(), attendance.getDurationSec());
    }

    private static AttendanceSummary combine(AttendanceSummary first, AttendanceSummary second) {
      Instant joinedAt =
          first.joinedAt().isBefore(second.joinedAt()) ? first.joinedAt() : second.joinedAt();
      Instant leftAt = latest(first.leftAt(), second.leftAt());
      Integer durationSeconds = sum(first.durationSeconds(), second.durationSeconds());
      return new AttendanceSummary(joinedAt, leftAt, durationSeconds);
    }

    private static Instant latest(Instant first, Instant second) {
      if (first == null) {
        return second;
      }
      if (second == null) {
        return first;
      }
      return first.isAfter(second) ? first : second;
    }

    private static Integer sum(Integer first, Integer second) {
      if (first == null) {
        return second;
      }
      if (second == null) {
        return first;
      }
      return first + second;
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private boolean isApiProvisioned(LiveClass liveClass) {
    return liveClass.getProvisioningMode() == LiveClassProvisioningMode.API_PROVISIONED;
  }

  private Integer positionOf(UUID liveClassId) {
    LiveClass liveClass = liveClassRepository.findById(liveClassId).orElse(null);
    if (liveClass == null) {
      return null;
    }
    return sectionItemRepository
        .findByItemTypeAndItemId(SectionItemType.LIVE_CLASS, liveClass.getSlot().getId())
        .map(SectionItem::getPosition)
        .orElse(null);
  }

  private User primaryInstructor(Course course) {
    List<CourseInstructor> instructors = courseInstructorRepository.findByCourseId(course.getId());
    return instructors.stream()
        .filter(i -> i.getRole() == com.gii.common.enums.InstructorRole.PRIMARY)
        .findFirst()
        .or(() -> instructors.stream().findFirst())
        .map(CourseInstructor::getInstructor)
        .orElse(null);
  }

  private String primaryInstructorName(Course course) {
    User instructor = primaryInstructor(course);
    return instructor != null ? instructor.getFullName() : null;
  }

  private String primaryInstructorEmail(Course course) {
    User instructor = primaryInstructor(course);
    return instructor != null ? instructor.getEmail() : null;
  }
}
