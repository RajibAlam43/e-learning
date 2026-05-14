package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateLiveClassRequest;
import com.gii.api.model.request.admin.UpdateLiveClassRequest;
import com.gii.api.model.response.admin.AdminLiveClassDetailResponse;
import com.gii.api.model.response.admin.AdminLiveClassRegistrantResponse;
import com.gii.api.model.response.admin.AdminLiveClassStartResponse;
import com.gii.api.model.response.admin.AdminLiveClassSummaryResponse;
import com.gii.api.service.live.LiveMeetingCreateRequest;
import com.gii.api.service.live.LiveMeetingCreateResult;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
  public List<AdminLiveClassSummaryResponse> list() {
    return liveClassRepository.findAll().stream().map(this::toSummary).toList();
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
            .description(request.description())
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
    if (request.title() != null && !request.title().isBlank()) {
      liveClass.setTitle(request.title().trim());
    }
    if (request.description() != null) {
      liveClass.setDescription(request.description());
    }
    if (request.startsAt() != null) {
      liveClass.setStartsAt(request.startsAt());
    }
    if (request.endsAt() != null) {
      liveClass.setEndsAt(request.endsAt());
    }
    validateTimeRange(liveClass.getStartsAt(), liveClass.getEndsAt());
    if (request.status() != null) {
      liveClass.setStatus(parseStatus(request.status()));
    }
    return toDetail(liveClassRepository.save(liveClass));
  }

  public AdminLiveClassStartResponse start(UUID liveClassId) {
    LiveClass liveClass =
        liveClassRepository
            .findById(liveClassId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Live class not found"));
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

  private AdminLiveClassSummaryResponse toSummary(LiveClass liveClass) {
    String instructorName =
        liveClass.getInstructor() != null ? liveClass.getInstructor().getFullName() : null;
    int registered =
        registrantRepository.findByLiveClassIdOrderByCreatedAtAsc(liveClass.getId()).size();
    return AdminLiveClassSummaryResponse.builder()
        .liveClassId(liveClass.getId())
        .title(liveClass.getTitle())
        .courseName(liveClass.getCourse().getTitle())
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
        .description(liveClass.getDescription())
        .courseId(liveClass.getCourse().getId())
        .courseName(liveClass.getCourse().getTitle())
        .sectionId(liveClass.getSection().getId())
        .sectionTitle(liveClass.getSection().getTitle())
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

  private LiveClassStatus parseStatus(String value) {
    try {
      return LiveClassStatus.valueOf(value.trim().toUpperCase());
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid live class status");
    }
  }
}
