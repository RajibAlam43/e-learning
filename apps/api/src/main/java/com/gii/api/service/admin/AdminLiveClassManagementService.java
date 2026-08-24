package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateLiveClassItemRequest;
import com.gii.api.model.request.admin.CreateLiveClassRequest;
import com.gii.api.model.request.admin.ScheduleExternalLiveClassRequest;
import com.gii.api.model.request.admin.ScheduleLiveClassRequest;
import com.gii.api.model.request.admin.UpdateLiveClassRequest;
import com.gii.api.model.response.admin.AdminLiveClassDetailResponse;
import com.gii.api.model.response.admin.AdminLiveClassItemResponse;
import com.gii.api.model.response.admin.AdminLiveClassRegistrantResponse;
import com.gii.api.model.response.admin.AdminLiveClassStartResponse;
import com.gii.api.model.response.admin.AdminLiveClassSummaryResponse;
import com.gii.api.service.course.CourseTemplateMutationGuard;
import com.gii.api.service.live.LiveMeetingCancelRequest;
import com.gii.api.service.live.LiveMeetingCreateRequest;
import com.gii.api.service.live.LiveMeetingCreateResult;
import com.gii.api.service.live.LiveMeetingProvisioningService;
import com.gii.api.service.live.LiveMeetingUpdateRequest;
import com.gii.api.service.progress.EnrollmentCompletionService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.SectionItem;
import com.gii.common.entity.live.LiveClass;
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
import com.gii.common.repository.live.LiveClassRegistrantRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.live.LiveClassSlotRepository;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
  private final SectionItemRepository sectionItemRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final LiveClassSlotRepository liveClassSlotRepository;
  private final LiveMeetingProvisioningService liveMeetingProvisioningService;
  private final CourseTemplateMutationGuard templateMutationGuard;
  private final EnrollmentCompletionService enrollmentCompletionService;

  @Transactional(readOnly = true)
  public Page<AdminLiveClassSummaryResponse> list(
      int page, int size, List<LiveClassStatus> statuses) {
    if (page < 0 || size < 1 || size > 100) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
    }
    PageRequest pageable =
        PageRequest.of(page, size, Sort.by(Sort.Order.asc("startsAt"), Sort.Order.asc("id")));
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
                    liveClassIds, com.gii.common.enums.LiveClassRegistrantStatus.APPROVED)
                .stream()
                .collect(
                    Collectors.toMap(row -> (UUID) row[0], row -> ((Number) row[1]).intValue()));
    return liveClasses.map(
        liveClass -> toSummary(liveClass, registrantCounts.getOrDefault(liveClass.getId(), 0)));
  }

  public AdminLiveClassDetailResponse create(UUID courseId, CreateLiveClassRequest request) {
    Course course =
        courseRepository
            .findByIdForUpdate(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    CourseSection section =
        sectionRepository
            .findById(request.sectionId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
    validateHierarchy(course, section);
    templateMutationGuard.requireDraft(course.getTemplateVersion());
    validateSupportedProvider(request.provider());
    validateTimeRange(request.startsAt(), request.endsAt());
    validateOfferingWindow(course, request.startsAt(), request.endsAt());
    validateCapacity(request.maxCapacity());
    ensureNoProviderOverlap(request.provider(), request.startsAt(), request.endsAt());
    int position = resolveCreatePosition(section.getId(), request.position());

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
      LiveClassSlot slot =
          liveClassSlotRepository.saveAndFlush(
              LiveClassSlot.builder()
                  .section(section)
                  .title(request.title().trim())
                  .titleEn(request.titleEn())
                  .description(request.description())
                  .descriptionEn(request.descriptionEn())
                  .expectedDurationMinutes(
                      Math.toIntExact(
                          Duration.between(request.startsAt(), request.endsAt()).toMinutes()))
                  .isMandatory(true)
                  .build());
      LiveClass saved =
          liveClassRepository.saveAndFlush(
              LiveClass.builder()
                  .course(course)
                  .slot(slot)
                  .provider(request.provider())
                  .providerMeetingId(meeting.meetingId())
                  .hostStartUrl(meeting.hostStartUrl())
                  .participantJoinUrl(meeting.participantJoinUrl())
                  .provisioningMode(LiveClassProvisioningMode.API_PROVISIONED)
                  .maxCapacity(request.maxCapacity())
                  .status(LiveClassStatus.SCHEDULED)
                  .startsAt(request.startsAt())
                  .endsAt(request.endsAt())
                  .build());
      sectionItemRepository.saveAndFlush(
          SectionItem.builder()
              .section(section)
              .itemType(SectionItemType.LIVE_CLASS)
              .itemId(slot.getId())
              .position(position)
              .build());
      return toDetail(saved);
    } catch (RuntimeException persistenceFailure) {
      compensateCreatedMeeting(request.provider(), meeting.meetingId(), persistenceFailure);
      throw persistenceFailure;
    }
  }

  public AdminLiveClassItemResponse createItem(UUID courseId, CreateLiveClassItemRequest request) {
    Course course = requireCourse(courseId);
    CourseSection section = requireSection(request.sectionId());
    validateHierarchy(course, section);
    templateMutationGuard.requireDraft(course.getTemplateVersion());
    int position = resolveCreatePosition(section.getId(), request.position());
    LiveClassSlot slot =
        liveClassSlotRepository.save(
            LiveClassSlot.builder()
                .section(section)
                .title(request.title().trim())
                .titleEn(request.titleEn())
                .description(request.description())
                .descriptionEn(request.descriptionEn())
                .expectedDurationMinutes(request.expectedDurationMinutes())
                .isMandatory(!Boolean.FALSE.equals(request.isMandatory()))
                .build());
    sectionItemRepository.save(
        SectionItem.builder()
            .section(section)
            .itemType(SectionItemType.LIVE_CLASS)
            .itemId(slot.getId())
            .position(position)
            .build());
    return toItemResponse(slot, position, false);
  }

  public AdminLiveClassDetailResponse schedule(
      UUID courseId, UUID liveClassItemId, ScheduleLiveClassRequest request) {
    Course course = requireCourse(courseId);
    final LiveClassSlot slot = requireSchedulableSlot(course, courseId, liveClassItemId);
    validateSupportedProvider(request.provider());
    validateTimeRange(request.startsAt(), request.endsAt());
    validateOfferingWindow(course, request.startsAt(), request.endsAt());
    validateCapacity(request.maxCapacity());
    ensureNoProviderOverlap(request.provider(), request.startsAt(), request.endsAt());
    LiveMeetingCreateResult meeting =
        liveMeetingProvisioningService.createMeeting(
            LiveMeetingCreateRequest.builder()
                .provider(request.provider())
                .title(slot.getTitle())
                .description(slot.getDescription())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .maxCapacity(request.maxCapacity())
                .build());
    try {
      LiveClass liveClass =
          liveClassRepository.saveAndFlush(
              LiveClass.builder()
                  .course(course)
                  .slot(slot)
                  .provider(request.provider())
                  .providerMeetingId(meeting.meetingId())
                  .hostStartUrl(meeting.hostStartUrl())
                  .participantJoinUrl(meeting.participantJoinUrl())
                  .provisioningMode(LiveClassProvisioningMode.API_PROVISIONED)
                  .maxCapacity(request.maxCapacity())
                  .status(LiveClassStatus.SCHEDULED)
                  .startsAt(request.startsAt())
                  .endsAt(request.endsAt())
                  .build());
      return toDetail(liveClass);
    } catch (RuntimeException persistenceFailure) {
      compensateCreatedMeeting(request.provider(), meeting.meetingId(), persistenceFailure);
      throw persistenceFailure;
    }
  }

  public AdminLiveClassDetailResponse scheduleExternal(
      UUID courseId, UUID liveClassItemId, ScheduleExternalLiveClassRequest request) {
    Course course = requireCourse(courseId);
    final LiveClassSlot slot = requireSchedulableSlot(course, courseId, liveClassItemId);
    validateTimeRange(request.startsAt(), request.endsAt());
    validateOfferingWindow(course, request.startsAt(), request.endsAt());
    validateCapacity(request.maxCapacity());
    if (request.provider() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider is required");
    }

    String participantJoinUrl = requireHttpsUrl(request.participantJoinUrl(), "participantJoinUrl");
    String hostStartUrl =
        isBlank(request.hostStartUrl())
            ? participantJoinUrl
            : requireHttpsUrl(request.hostStartUrl(), "hostStartUrl");
    String meetingId = trimToNull(request.meetingId());

    LiveClass liveClass =
        liveClassRepository.save(
            LiveClass.builder()
                .course(course)
                .slot(slot)
                .provider(request.provider())
                .providerMeetingId(meetingId)
                .hostStartUrl(hostStartUrl)
                .participantJoinUrl(participantJoinUrl)
                .provisioningMode(LiveClassProvisioningMode.EXTERNAL_URL)
                .maxCapacity(request.maxCapacity())
                .status(LiveClassStatus.SCHEDULED)
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .build());
    return toDetail(liveClass);
  }

  private LiveClassSlot requireSchedulableSlot(Course course, UUID courseId, UUID liveClassItemId) {
    LiveClassSlot slot =
        liveClassSlotRepository
            .findByIdForUpdate(liveClassItemId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Live class item not found"));
    validateHierarchy(course, slot.getSection());
    if (liveClassRepository.existsByCourseIdAndSlotId(courseId, slot.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Live class item is already scheduled for this course");
    }
    return slot;
  }

  private Course requireCourse(UUID courseId) {
    return courseRepository
        .findByIdForUpdate(courseId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
  }

  private CourseSection requireSection(UUID sectionId) {
    return sectionRepository
        .findById(sectionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
  }

  private AdminLiveClassItemResponse toItemResponse(
      LiveClassSlot slot, int position, boolean scheduled) {
    return AdminLiveClassItemResponse.builder()
        .liveClassItemId(slot.getId())
        .liveClassId(null)
        .sectionId(slot.getSection().getId())
        .position(position)
        .title(slot.getTitle())
        .titleEn(slot.getTitleEn())
        .description(slot.getDescription())
        .descriptionEn(slot.getDescriptionEn())
        .expectedDurationMinutes(slot.getExpectedDurationMinutes())
        .isMandatory(slot.getIsMandatory())
        .scheduled(scheduled)
        .build();
  }

  public AdminLiveClassDetailResponse update(UUID liveClassId, UpdateLiveClassRequest request) {
    LiveClass liveClass =
        liveClassRepository
            .findById(liveClassId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Live class not found"));
    final Course course =
        courseRepository.findByIdForUpdate(liveClass.getCourse().getId()).orElseThrow();
    boolean mutatingMetadata =
        request.title() != null
            || request.titleEn() != null
            || request.description() != null
            || request.descriptionEn() != null
            || request.startsAt() != null
            || request.endsAt() != null;
    if (request.position() != null) {
      templateMutationGuard.requireDraft(liveClass.getSection().getTemplateVersion());
    }
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
      validateOfferingWindow(course, startsAt, endsAt);
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
      LiveClassStatus nextStatus = parseStatus(request.status());
      validateStatusTransitionForUpdate(liveClass.getStatus(), nextStatus);
      liveClass.setStatus(nextStatus);
      if (nextStatus == LiveClassStatus.COMPLETED
          || nextStatus == LiveClassStatus.CANCELLED
          || nextStatus == LiveClassStatus.FAILED) {
        liveClassRepository.saveAndFlush(liveClass);
        enrollmentCompletionService.refreshCourse(liveClass.getCourse().getId());
      }
    }
    if (request.position() != null) {
      updatePosition(liveClass, request.position());
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
    if ((isApiProvisioned(liveClass) && isBlank(liveClass.effectiveMeetingId()))
        || isBlank(liveClass.effectiveParticipantJoinUrl())
        || isBlank(liveClass.effectiveHostStartUrl())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Live class meeting is not provisioned");
    }
    liveClass.setStatus(LiveClassStatus.LIVE);
    LiveClass saved = liveClassRepository.save(liveClass);
    long approvedRegistrants =
        registrantRepository.countByLiveClassIdAndStatus(
            saved.getId(), LiveClassRegistrantStatus.APPROVED);
    return AdminLiveClassStartResponse.builder()
        .liveClassId(saved.getId())
        .title(saved.getTitle())
        .provider(saved.getProvider())
        .hostStartUrl(saved.effectiveHostStartUrl())
        .meetingId(saved.effectiveMeetingId())
        .startsAt(saved.getStartsAt())
        .endsAt(saved.getEndsAt())
        .status(saved.getStatus().name())
        .approvedRegistrants(Math.toIntExact(approvedRegistrants))
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
    LiveClass cancelled = liveClassRepository.saveAndFlush(liveClass);
    enrollmentCompletionService.refreshCourse(cancelled.getCourse().getId());
    return toDetail(cancelled);
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

  private AdminLiveClassSummaryResponse toSummary(LiveClass liveClass, int approvedRegistrants) {
    User instructor = primaryInstructor(liveClass.getCourse());
    String instructorName = instructor != null ? instructor.getFullName() : null;
    return AdminLiveClassSummaryResponse.builder()
        .liveClassId(liveClass.getId())
        .title(liveClass.getTitle())
        .titleEn(liveClass.getTitleEn())
        .courseName(liveClass.getCourse().getTitle())
        .courseNameEn(liveClass.getCourse().getTitleEn())
        .instructorName(instructorName)
        .status(liveClass.getStatus().name())
        .startsAt(liveClass.getStartsAt())
        .approvedRegistrants(approvedRegistrants)
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
        .position(positionOf(liveClass.getId()))
        .instructorId(primaryInstructorId(liveClass.getCourse()))
        .instructorName(primaryInstructorName(liveClass.getCourse()))
        .startsAt(liveClass.getStartsAt())
        .endsAt(liveClass.getEndsAt())
        .provider(liveClass.getProvider())
        .provisioningMode(liveClass.getProvisioningMode())
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
    if (!section.getTemplateVersion().getId().equals(course.getTemplateVersion().getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Section does not belong to course");
    }
  }

  private int resolveCreatePosition(UUID sectionId, Integer requestedPosition) {
    int position =
        requestedPosition != null
            ? requestedPosition
            : sectionItemRepository.findMaxPositionBySectionId(sectionId) + 1;
    ensurePositionAvailable(sectionId, position, null);
    return position;
  }

  private void updatePosition(LiveClass liveClass, Integer requestedPosition) {
    ensurePositionAvailable(
        liveClass.getSection().getId(), requestedPosition, liveClass.getSlot().getId());
    SectionItem item =
        sectionItemRepository
            .findByItemTypeAndItemId(SectionItemType.LIVE_CLASS, liveClass.getSlot().getId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Section item missing"));
    item.setPosition(requestedPosition);
    sectionItemRepository.save(item);
  }

  private void ensurePositionAvailable(
      UUID sectionId, Integer requestedPosition, UUID currentLiveClassId) {
    if (requestedPosition == null || requestedPosition <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "position must be positive");
    }
    sectionItemRepository
        .findBySectionIdAndPosition(sectionId, requestedPosition)
        .ifPresent(
            item -> {
              boolean sameLiveClass =
                  item.getItemType() == SectionItemType.LIVE_CLASS
                      && currentLiveClassId != null
                      && currentLiveClassId.equals(item.getItemId());
              if (!sameLiveClass) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Position is already used in this section");
              }
            });
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

  private UUID primaryInstructorId(Course course) {
    User instructor = primaryInstructor(course);
    return instructor != null ? instructor.getId() : null;
  }

  private String primaryInstructorName(Course course) {
    User instructor = primaryInstructor(course);
    return instructor != null ? instructor.getFullName() : null;
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

  private void validateOfferingWindow(Course course, Instant startsAt, Instant endsAt) {
    if (course.getStartsAt() != null && startsAt.isBefore(course.getStartsAt())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Live class cannot start before the course offering");
    }
    if (course.getEndsAt() != null && endsAt.isAfter(course.getEndsAt())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Live class cannot end after the course offering");
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

  private String requireHttpsUrl(String value, String fieldName) {
    if (isBlank(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
    }
    try {
      URI uri = URI.create(value.trim());
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || isBlank(uri.getHost())
          || uri.getUserInfo() != null) {
        throw new IllegalArgumentException("URL must use HTTPS and include a host");
      }
      return uri.toASCIIString();
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, fieldName + " must be a valid HTTPS URL");
    }
  }

  private String trimToNull(String value) {
    return isBlank(value) ? null : value.trim();
  }

  private boolean isApiProvisioned(LiveClass liveClass) {
    return liveClass.getProvisioningMode() == LiveClassProvisioningMode.API_PROVISIONED;
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
