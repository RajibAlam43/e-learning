package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateLiveClassRequest;
import com.gii.api.model.request.admin.UpdateLiveClassRequest;
import com.gii.api.model.response.admin.AdminLiveClassDetailResponse;
import com.gii.api.model.response.admin.AdminLiveClassRegistrantResponse;
import com.gii.api.model.response.admin.AdminLiveClassStartResponse;
import com.gii.api.model.response.admin.AdminLiveClassSummaryResponse;
import com.gii.api.service.live.LiveMeetingCreateRequest;
import com.gii.api.service.live.LiveMeetingCreateResult;
import com.gii.api.service.live.LiveMeetingUpdateRequest;
import com.gii.api.service.live.LiveMeetingCancelRequest;
import com.gii.api.service.live.LiveMeetingProvisioningService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassRegistrant;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.live.LiveClassRegistrantRepository;
import com.gii.common.repository.live.LiveClassRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminLiveClassManagementService {
  private static final Duration CREATE_LEAD_TIME = Duration.ofMinutes(2);
  private static final int MAX_CAPACITY_LIMIT = 1000;

  private final LiveClassRepository liveClassRepository;
  private final LiveClassRegistrantRepository registrantRepository;
  private final CourseRepository courseRepository;
  private final CourseSectionRepository sectionRepository;
  private final LiveMeetingProvisioningService liveMeetingProvisioningService;

  @Transactional(readOnly = true)
  public Page<AdminLiveClassSummaryResponse> list(
      int page, int size, List<LiveClassStatus> statuses) {
    if (page < 0 || size < 1 || size > 100) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
    }
    PageRequest pageable =
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startsAt"));
    Page<LiveClass> liveClasses =
        statuses == null || statuses.isEmpty()
            ? liveClassRepository.findAdminPage(pageable)
            : liveClassRepository.findAdminPageByStatuses(statuses, pageable);
    List<UUID> liveClassIds = liveClasses.stream().map(LiveClass::getId).toList();
    Map<UUID, Integer> registrantCounts =
        liveClassIds.isEmpty()
            ? Map.of()
            : registrantRepository
                .countByLiveClassIdsAndStatus(
                    liveClassIds,
                    com.gii.common.enums.LiveClassRegistrantStatus.APPROVED)
                .stream()
                .collect(
                    Collectors.toMap(
                        row -> (UUID) row[0], row -> ((Number) row[1]).intValue()));
    return liveClasses.map(
        liveClass -> toSummary(liveClass, registrantCounts.getOrDefault(liveClass.getId(), 0)));
  }

  public AdminLiveClassDetailResponse create(UUID courseId, CreateLiveClassRequest request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    CourseSection section =
        sectionRepository
            .findById(request.sectionId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
    validateHierarchy(course, section);
    validateSupportedProvider(request.provider());
    validateTimeRange(request.startsAt(), request.endsAt());
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
            .course(course)
            .section(section)
            .instructor(null)
            .title(request.title().trim())
            .titleEn(request.titleEn())
            .description(request.description())
            .descriptionEn(request.descriptionEn())
            .provider(request.provider())
            .providerMeetingId(meeting.meetingId())
            .hostStartUrl(meeting.hostStartUrl())
            .participantJoinUrl(meeting.participantJoinUrl())
            .maxCapacity(request.maxCapacity())
            .status(LiveClassStatus.SCHEDULED)
            .startsAt(request.startsAt())
            .endsAt(request.endsAt())
            .build();
    return toDetail(liveClassRepository.save(liveClass));
  }

  public AdminLiveClassDetailResponse update(UUID liveClassId, UpdateLiveClassRequest request) {
    LiveClass liveClass =
        liveClassRepository
            .findById(liveClassId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Live class not found"));
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
      validateTimeRange(startsAt, endsAt);
      ensureNoProviderOverlap(liveClass.getProvider(), startsAt, endsAt, liveClass.getId());
      syncProviderUpdate(
          liveClass,
          request.title() != null && !request.title().isBlank() ? request.title().trim() : liveClass.getTitle(),
          request.description() != null ? request.description() : liveClass.getDescription(),
          startsAt,
          endsAt);
      liveClass.setStartsAt(startsAt);
      liveClass.setEndsAt(endsAt);
    }
    if (request.startsAt() == null && request.endsAt() == null && (request.title() != null || request.description() != null)) {
      syncProviderUpdate(
          liveClass,
          request.title() != null && !request.title().isBlank() ? request.title().trim() : liveClass.getTitle(),
          request.description() != null ? request.description() : liveClass.getDescription(),
          liveClass.getStartsAt(),
          liveClass.getEndsAt());
    }
    if (request.status() != null) {
      LiveClassStatus nextStatus = parseStatus(request.status());
      validateStatusTransitionForUpdate(liveClass.getStatus(), nextStatus);
      liveClass.setStatus(nextStatus);
    }
    return toDetail(liveClassRepository.save(liveClass));
  }

  public AdminLiveClassStartResponse start(UUID liveClassId) {
    LiveClass liveClass =
        liveClassRepository
            .findById(liveClassId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Live class not found"));
    if (liveClass.getStatus() != LiveClassStatus.SCHEDULED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Only scheduled classes can be started");
    }
    if (isBlank(liveClass.effectiveMeetingId())
        || isBlank(liveClass.effectiveParticipantJoinUrl())
        || isBlank(liveClass.effectiveHostStartUrl())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Live class meeting is not provisioned");
    }
    liveClass.setStatus(LiveClassStatus.LIVE);
    LiveClass saved = liveClassRepository.save(liveClass);
    int registered =
        registrantRepository.findByLiveClassIdOrderByCreatedAtAsc(saved.getId()).size();
    return AdminLiveClassStartResponse.builder()
        .liveClassId(saved.getId())
        .title(saved.getTitle())
        .provider(saved.getProvider())
        .hostStartUrl(saved.effectiveHostStartUrl())
        .meetingId(saved.effectiveMeetingId())
        .startsAt(saved.getStartsAt())
        .endsAt(saved.getEndsAt())
        .status(saved.getStatus().name())
        .registeredStudents(registered)
        .recordingEnabled(Boolean.FALSE)
        .build();
  }

  public AdminLiveClassDetailResponse cancel(UUID liveClassId) {
    LiveClass liveClass =
        liveClassRepository
            .findById(liveClassId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Live class not found"));
    if (liveClass.getStatus() != LiveClassStatus.SCHEDULED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Only scheduled classes can be cancelled");
    }
    syncProviderCancel(liveClass);
    liveClass.setStatus(LiveClassStatus.CANCELLED);
    return toDetail(liveClassRepository.save(liveClass));
  }

  private void syncProviderUpdate(
      LiveClass liveClass, String title, String description, Instant startsAt, Instant endsAt) {
    if (isBlank(liveClass.effectiveMeetingId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Live class meeting is not provisioned");
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
    if (isBlank(liveClass.effectiveMeetingId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Live class meeting is not provisioned");
    }
    liveMeetingProvisioningService.cancelMeeting(
        LiveMeetingCancelRequest.builder()
            .provider(liveClass.getProvider())
            .providerMeetingId(liveClass.effectiveMeetingId())
            .build());
  }

  private AdminLiveClassSummaryResponse toSummary(LiveClass liveClass, int registered) {
    String instructorName =
        liveClass.getInstructor() != null ? liveClass.getInstructor().getFullName() : null;
    return AdminLiveClassSummaryResponse.builder()
        .liveClassId(liveClass.getId())
        .title(liveClass.getTitle())
        .titleEn(liveClass.getTitleEn())
        .courseName(liveClass.getCourse().getTitle())
        .courseNameEn(liveClass.getCourse().getTitleEn())
        .instructorName(instructorName)
        .status(liveClass.getStatus().name())
        .startsAt(liveClass.getStartsAt())
        .registeredStudents(registered)
        .createdAt(liveClass.getCreatedAt())
        .build();
  }

  private AdminLiveClassDetailResponse toDetail(LiveClass liveClass) {
    List<AdminLiveClassRegistrantResponse> registrants =
        registrantRepository.findByLiveClassIdOrderByCreatedAtAsc(liveClass.getId()).stream()
            .map(this::toRegistrantResponse)
            .toList();
    return AdminLiveClassDetailResponse.builder()
        .liveClassId(liveClass.getId())
        .title(liveClass.getTitle())
        .titleEn(liveClass.getTitleEn())
        .description(liveClass.getDescription())
        .descriptionEn(liveClass.getDescriptionEn())
        .courseId(liveClass.getCourse().getId())
        .courseName(liveClass.getCourse().getTitle())
        .courseNameEn(liveClass.getCourse().getTitleEn())
        .sectionId(liveClass.getSection().getId())
        .sectionTitle(liveClass.getSection().getTitle())
        .sectionTitleEn(liveClass.getSection().getTitleEn())
        .instructorId(liveClass.getInstructor() != null ? liveClass.getInstructor().getId() : null)
        .instructorName(
            liveClass.getInstructor() != null ? liveClass.getInstructor().getFullName() : null)
        .startsAt(liveClass.getStartsAt())
        .endsAt(liveClass.getEndsAt())
        .provider(liveClass.getProvider())
        .status(liveClass.getStatus().name())
        .meetingId(liveClass.effectiveMeetingId())
        .hostStartUrl(liveClass.effectiveHostStartUrl())
        .joinUrl(liveClass.effectiveParticipantJoinUrl())
        .createdAt(liveClass.getCreatedAt())
        .updatedAt(liveClass.getUpdatedAt())
        .registrants(registrants)
        .build();
  }

  private AdminLiveClassRegistrantResponse toRegistrantResponse(LiveClassRegistrant registrant) {
    return AdminLiveClassRegistrantResponse.builder()
        .registrantId(registrant.getId())
        .userId(registrant.getUser().getId())
        .studentName(registrant.getUser().getFullName())
        .studentEmail(registrant.getUser().getEmail())
        .status(registrant.getStatus().name())
        .providerRegistrantId(registrant.getProviderRegistrantId())
        .attended(null)
        .joinedAt(null)
        .leftAt(null)
        .durationSeconds(null)
        .build();
  }

  private void validateHierarchy(Course course, CourseSection section) {
    if (!section.getCourse().getId().equals(course.getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Section does not belong to course");
    }
  }

  private void validateTimeRange(Instant startsAt, Instant endsAt) {
    if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid live class time range");
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

  private void ensureNoProviderOverlap(
      LiveClassProvider provider, Instant startsAt, Instant endsAt, UUID excludingLiveClassId) {
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

  private LiveClassStatus parseStatus(String value) {
    try {
      return LiveClassStatus.valueOf(value.trim().toUpperCase());
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid live class status");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
