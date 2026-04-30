package com.gii.api.service.instructor;

import com.gii.api.model.request.instructor.CreateLiveClassRequest;
import com.gii.api.model.request.instructor.UpdateLiveClassRequest;
import com.gii.api.model.response.instructor.InstructorLiveClassResponse;
import com.gii.api.model.response.instructor.InstructorLiveClassStartResponse;
import com.gii.api.model.response.instructor.LiveClassRegistrantSummaryResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassAttendance;
import com.gii.common.entity.live.LiveClassRegistrant;
import com.gii.common.entity.user.User;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.live.LiveClassAttendanceRepository;
import com.gii.common.repository.live.LiveClassRegistrantRepository;
import com.gii.common.repository.live.LiveClassRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

  private final CurrentUserService currentUserService;
  private final CourseRepository courseRepository;
  private final CourseSectionRepository courseSectionRepository;
  private final LessonRepository lessonRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final LiveClassRepository liveClassRepository;
  private final LiveClassRegistrantRepository liveClassRegistrantRepository;
  private final LiveClassAttendanceRepository liveClassAttendanceRepository;

  public InstructorLiveClassResponse create(
      UUID courseId, CreateLiveClassRequest request, Authentication authentication) {
    User instructor = currentUserService.getCurrentUser(authentication);
    final Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    ensureInstructorAssigned(courseId, instructor.getId());

    CourseSection section =
        courseSectionRepository
            .findById(request.sectionId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section not found"));
    Lesson lesson =
        lessonRepository
            .findById(request.lessonId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesson not found"));

    validateCourseMapping(courseId, section, lesson);
    validateSchedule(request.startsAt(), request.endsAt());

    LiveClass liveClass =
        LiveClass.builder()
            .course(course)
            .section(section)
            .lesson(lesson)
            .instructor(instructor)
            .title(request.title().trim())
            .description(request.description())
            // Provider-agnostic fields (legacy zoomMeetingLink still accepted as fallback input).
            .provider(request.provider() != null ? request.provider() : LiveClassProvider.ZOOM)
            .providerMeetingId(request.providerMeetingId())
            .hostStartUrl(request.hostStartUrl())
            .participantJoinUrl(
                firstNonBlank(request.participantJoinUrl(), request.zoomMeetingLink()))
            // Legacy fields are mirrored for backward compatibility during migration.
            .zoomMeetingId(request.providerMeetingId())
            .zoomStartUrl(request.hostStartUrl())
            .zoomJoinUrl(firstNonBlank(request.participantJoinUrl(), request.zoomMeetingLink()))
            .startsAt(request.startsAt())
            .endsAt(request.endsAt())
            .status(LiveClassStatus.SCHEDULED)
            .createdBy(instructor)
            .build();

    LiveClass saved = liveClassRepository.save(liveClass);
    return toLiveClassResponse(saved);
  }

  public InstructorLiveClassStartResponse start(UUID liveClassId, Authentication authentication) {
    User instructor = currentUserService.getCurrentUser(authentication);
    LiveClass liveClass = requireOwnedLiveClass(liveClassId, instructor.getId());

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
        .zoomStartUrl(liveClass.effectiveHostStartUrl())
        .zoomMeetingId(liveClass.effectiveMeetingId())
        .zoomPassword(null)
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
    User instructor = currentUserService.getCurrentUser(authentication);
    LiveClass liveClass = requireOwnedLiveClass(liveClassId, instructor.getId());

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
    User instructor = currentUserService.getCurrentUser(authentication);
    LiveClass liveClass = requireOwnedLiveClass(liveClassId, instructor.getId());

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
    LiveClass liveClass =
        liveClassRepository
            .findById(liveClassId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Live class not found"));
    if (liveClass.getInstructor() == null
        || !liveClass.getInstructor().getId().equals(instructorId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for this class");
    }
    return liveClass;
  }

  private void ensureInstructorAssigned(UUID courseId, UUID instructorId) {
    boolean assigned =
        courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, instructorId);
    if (!assigned) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this course");
    }
  }

  private void validateCourseMapping(UUID courseId, CourseSection section, Lesson lesson) {
    if (!section.getCourse().getId().equals(courseId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Section does not belong to course");
    }
    if (!lesson.getCourse().getId().equals(courseId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesson does not belong to course");
    }
    if (!lesson.getSection().getId().equals(section.getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Lesson does not belong to section");
    }
  }

  private void validateSchedule(Instant startsAt, Instant endsAt) {
    if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid schedule");
    }
  }

  private InstructorLiveClassResponse toLiveClassResponse(LiveClass liveClass) {
    List<LiveClassRegistrant> registrants =
        liveClassRegistrantRepository.findByLiveClassIdOrderByCreatedAtAsc(liveClass.getId());
    List<LiveClassAttendance> attendanceRows =
        liveClassAttendanceRepository.findByLiveClassId(liveClass.getId());

    List<LiveClassRegistrantSummaryResponse> registrantSummaries =
        registrants.stream()
            .map(
                registrant -> {
                  LiveClassAttendance attendance =
                      attendanceRows.stream()
                          .filter(
                              a ->
                                  a.getUser() != null
                                      && a.getUser().getId().equals(registrant.getUser().getId()))
                          .findFirst()
                          .orElse(null);

                  return LiveClassRegistrantSummaryResponse.builder()
                      .registrantId(registrant.getId())
                      .userId(registrant.getUser().getId())
                      .studentName(registrant.getUser().getFullName())
                      .studentEmail(registrant.getUser().getEmail())
                      .status(registrant.getStatus())
                      .zoomRegistrantId(registrant.getZoomRegistrantId())
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
        .lessonId(liveClass.getLesson().getId())
        .lessonTitle(liveClass.getLesson().getTitle())
        .instructorName(
            liveClass.getInstructor() != null ? liveClass.getInstructor().getFullName() : null)
        .instructorEmail(
            liveClass.getInstructor() != null ? liveClass.getInstructor().getEmail() : null)
        .startsAt(liveClass.getStartsAt())
        .endsAt(liveClass.getEndsAt())
        .durationMinutes(
            Duration.between(liveClass.getStartsAt(), liveClass.getEndsAt()).toMinutes())
        .timezone(null)
        .status(liveClass.getStatus())
        .zoomMeetingId(liveClass.effectiveMeetingId())
        .zoomStartUrl(liveClass.effectiveHostStartUrl())
        .zoomJoinUrl(liveClass.effectiveParticipantJoinUrl())
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

  private String firstNonBlank(String primary, String fallback) {
    if (primary != null && !primary.isBlank()) {
      return primary;
    }
    return fallback;
  }
}
