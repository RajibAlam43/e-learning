package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateLiveClassRequest;
import com.gii.api.model.request.admin.UpdateLiveClassRequest;
import com.gii.api.model.response.admin.AdminLiveClassDetailResponse;
import com.gii.api.model.response.admin.AdminLiveClassRegistrantResponse;
import com.gii.api.model.response.admin.AdminLiveClassStartResponse;
import com.gii.api.model.response.admin.AdminLiveClassSummaryResponse;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassRegistrant;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.live.LiveClassRegistrantRepository;
import com.gii.common.repository.live.LiveClassRepository;
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

  private final LiveClassRepository liveClassRepository;
  private final LiveClassRegistrantRepository registrantRepository;
  private final CourseRepository courseRepository;
  private final CourseSectionRepository sectionRepository;
  private final LessonRepository lessonRepository;

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
    Lesson lesson =
        lessonRepository
            .findById(request.lessonId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    validateHierarchy(course, section, lesson);
    validateTimeRange(request.startsAt(), request.endsAt());

    LiveClass liveClass =
        LiveClass.builder()
            .course(course)
            .section(section)
            .lesson(lesson)
            .instructor(null)
            .title(request.title().trim())
            .description(request.description())
            .provider(LiveClassProvider.OTHER)
            .participantJoinUrl(request.zoomMeetingLink())
            .zoomJoinUrl(request.zoomMeetingLink())
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
        .zoomStartUrl(saved.effectiveHostStartUrl())
        .zoomMeetingId(saved.effectiveMeetingId())
        .zoomPassword(null)
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
        .lessonId(liveClass.getLesson().getId())
        .lessonTitle(liveClass.getLesson().getTitle())
        .instructorId(liveClass.getInstructor() != null ? liveClass.getInstructor().getId() : null)
        .instructorName(
            liveClass.getInstructor() != null ? liveClass.getInstructor().getFullName() : null)
        .startsAt(liveClass.getStartsAt())
        .endsAt(liveClass.getEndsAt())
        .status(liveClass.getStatus().name())
        .zoomMeetingId(liveClass.effectiveMeetingId())
        .zoomStartUrl(liveClass.effectiveHostStartUrl())
        .zoomJoinUrl(liveClass.effectiveParticipantJoinUrl())
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
        .zoomRegistrantId(registrant.getZoomRegistrantId())
        .attended(null)
        .joinedAt(null)
        .leftAt(null)
        .durationSeconds(null)
        .build();
  }

  private void validateHierarchy(Course course, CourseSection section, Lesson lesson) {
    if (!section.getCourse().getId().equals(course.getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Section does not belong to course");
    }
    if (!lesson.getSection().getId().equals(section.getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Lesson does not belong to section");
    }
  }

  private void validateTimeRange(java.time.Instant startsAt, java.time.Instant endsAt) {
    if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid live class time range");
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
