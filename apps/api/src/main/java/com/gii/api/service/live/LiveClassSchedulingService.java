package com.gii.api.service.live;

import com.gii.api.service.course.CourseInstructorResolver;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.user.User;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassProvisioningMode;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.live.LiveClassRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class LiveClassSchedulingService {
  public static final String MANUAL_HOSTS = "manualCoHostEmails";
  private static final List<LiveClassStatus> ACTIVE =
      List.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE);
  private final LiveClassRepository classes;
  private final CourseInstructorRepository instructors;
  private final CourseInstructorResolver instructorResolver;
  private final GoogleMeetLiveMeetingProvider google;
  private final GoogleMeetCredentialsService credentials;

  public User primaryInstructor(Course course) {
    return instructorResolver.primaryInstructor(course);
  }

  public List<String> courseCoHosts(Course course, LiveClassProvider provider) {
    if (provider != LiveClassProvider.GOOGLE_MEET || !credentials.sharedEnabled()) {
      return List.of();
    }
    return assignedTeacherEmails(course);
  }

  private List<String> assignedTeacherEmails(Course course) {
    List<String> emails =
        instructors.findByCourseId(course.getId()).stream()
            .map(assignment -> teacherEmail(assignment.getInstructor()))
            .distinct()
            .sorted()
            .toList();
    if (emails.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Assign at least one teacher with a Google account email before scheduling Google Meet");
    }
    checkHostLimit(emails);
    return emails;
  }

  public User leadForCreation(Course course, User schedulingTeacher, LiveClassProvider provider) {
    if (provider == LiveClassProvider.GOOGLE_MEET && !credentials.sharedEnabled()) {
      var primary =
          instructors.findByCourseId(course.getId()).stream()
              .filter(
                  assignment -> assignment.getRole() == com.gii.common.enums.InstructorRole.PRIMARY)
              .toList();
      if (primary.size() != 1) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Assign exactly one primary teacher to own Google Meet classes");
      }
      return primary.getFirst().getInstructor();
    }
    return schedulingTeacher != null ? schedulingTeacher : primaryInstructor(course);
  }

  public List<String> calendarGuests(Course course) {
    return instructors.findByCourseId(course.getId()).stream()
        .map(assignment -> assignment.getInstructor().getEmail())
        .filter(email -> email != null && email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
        .map(email -> email.toLowerCase(Locale.ROOT))
        .distinct()
        .sorted()
        .toList();
  }

  public List<String> calendarGuests(LiveClass liveClass) {
    List<String> emails = new ArrayList<>(calendarGuests(liveClass.getCourse()));
    Object manual =
        liveClass.getProviderMetadata() == null
            ? null
            : liveClass.getProviderMetadata().get(MANUAL_HOSTS);
    if (manual instanceof List<?> values) {
      values.forEach(email -> emails.add(email.toString()));
    }
    return emails.stream().distinct().sorted().toList();
  }

  public boolean canManageCoHosts(LiveClass liveClass) {
    return credentials.hasCoHosts(liveClass.getProviderMetadata())
        && (GoogleMeetLiveMeetingProvider.isSpaceName(liveClass.effectiveMeetingId())
            || credentials.sharedEnabled());
  }

  public String teacherEmail(User user) {
    String email = user == null ? null : user.getEmail();
    if (email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Every co-host needs an email matching their Google account;"
              + " phone-only accounts cannot be co-hosts");
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }

  public void ensureAvailable(
      LiveClassProvider provider,
      User teacher,
      Instant startsAt,
      Instant endsAt,
      UUID excludingId) {
    if (provider == LiveClassProvider.ZOOM) {
      classes.acquireProviderSchedulingLock(provider.ordinal());
      if (classes.existsProviderOverlapExcluding(provider, ACTIVE, startsAt, endsAt, excludingId)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Provider host account already has overlapping live class");
      }
    }
    if (teacher != null) {
      classes.acquireTeacherSchedulingLock(teacher.getId().hashCode());
      if (classes.existsTeacherOverlap(teacher.getId(), ACTIVE, startsAt, endsAt, excludingId)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "The lead teacher already has an overlapping live class");
      }
    }
  }

  public List<String> coHosts(LiveClass liveClass) {
    List<String> emails = new ArrayList<>(assignedTeacherEmails(liveClass.getCourse()));
    Map<String, Object> metadata = liveClass.getProviderMetadata();
    Object manual = metadata == null ? null : metadata.get(MANUAL_HOSTS);
    if (manual instanceof List<?> values) {
      for (Object email : values) {
        emails.add(email.toString().trim().toLowerCase(Locale.ROOT));
      }
    }
    List<String> result = emails.stream().distinct().sorted().toList();
    checkHostLimit(result);
    return result;
  }

  public void prepareGoogle(LiveClass liveClass) {
    if (liveClass.getProvider() != LiveClassProvider.GOOGLE_MEET
        || liveClass.getProvisioningMode() != LiveClassProvisioningMode.API_PROVISIONED) {
      return;
    }
    var request =
        LiveMeetingCreateRequest.builder()
            .provider(LiveClassProvider.GOOGLE_MEET)
            .title(liveClass.getTitle())
            .description(liveClass.getDescription())
            .startsAt(liveClass.getStartsAt())
            .endsAt(liveClass.getEndsAt())
            .calendarGuestEmails(calendarGuests(liveClass))
            .build();
    if (liveClass.getProviderMetadata() != null
        && "CALENDAR".equals(liveClass.getProviderMetadata().get("googleApiKind"))) {
      liveClass.setProviderMetadata(
          google.synchronizeCalendar(request, null, liveClass.getProviderMetadata()));
      return;
    }
    List<String> emails = canManageCoHosts(liveClass) ? coHosts(liveClass) : List.of();
    if (!GoogleMeetLiveMeetingProvider.isSpaceName(liveClass.effectiveMeetingId())) {
      if (liveClass.getStatus() != LiveClassStatus.SCHEDULED) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "An active legacy Google meeting cannot change links;"
                + " add co-hosts in Google Meet instead");
      }
      User lead =
          leadForCreation(
              liveClass.getCourse(), liveClass.getLeadInstructor(), LiveClassProvider.GOOGLE_MEET);
      ensureAvailable(
          liveClass.getProvider(),
          lead,
          liveClass.getStartsAt(),
          liveClass.getEndsAt(),
          liveClass.getId());
      LiveMeetingCreateResult meeting =
          google.create(
              LiveMeetingCreateRequest.builder()
                  .provider(LiveClassProvider.GOOGLE_MEET)
                  .title(liveClass.getTitle())
                  .description(liveClass.getDescription())
                  .startsAt(liveClass.getStartsAt())
                  .endsAt(liveClass.getEndsAt())
                  .maxCapacity(liveClass.getMaxCapacity())
                  .primaryTeacherId(lead == null ? null : lead.getId())
                  .calendarGuestEmails(calendarGuests(liveClass))
                  .coHostEmails(emails)
                  .build());
      Map<String, Object> metadata =
          liveClass.getProviderMetadata() == null
              ? new HashMap<>()
              : new HashMap<>(liveClass.getProviderMetadata());
      metadata.put("legacyCalendarEventId", liveClass.effectiveMeetingId());
      metadata.put("legacyMeetUrl", liveClass.effectiveParticipantJoinUrl());
      if (meeting.metadata() != null) {
        metadata.putAll(meeting.metadata());
      }
      liveClass.setLeadInstructor(lead);
      liveClass.setProviderMetadata(metadata);
      liveClass.setProviderMeetingId(meeting.meetingId());
      liveClass.setHostStartUrl(meeting.hostStartUrl());
      liveClass.setParticipantJoinUrl(meeting.participantJoinUrl());
    } else {
      liveClass.setProviderMetadata(
          google.synchronizeCoHosts(
              liveClass.effectiveMeetingId(), emails, liveClass.getProviderMetadata()));
    }
    if (liveClass.getProviderMetadata() != null
        && (credentials.calendarEnabled()
            || liveClass.getProviderMetadata().containsKey("googleCalendarEventId"))) {
      liveClass.setProviderMetadata(
          google.synchronizeCalendar(
              request, liveClass.effectiveParticipantJoinUrl(), liveClass.getProviderMetadata()));
    }
  }

  private void checkHostLimit(List<String> emails) {
    if (emails.size() > 25) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Course teachers and manually added co-hosts cannot exceed 25 Google accounts");
    }
  }
}
