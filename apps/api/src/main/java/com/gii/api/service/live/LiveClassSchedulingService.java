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

  public User primaryInstructor(Course course) {
    return instructorResolver.primaryInstructor(course);
  }

  public List<String> courseCoHosts(Course course, LiveClassProvider provider) {
    if (provider != LiveClassProvider.GOOGLE_MEET) {
      return List.of();
    }
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
    List<String> emails =
        new ArrayList<>(courseCoHosts(liveClass.getCourse(), liveClass.getProvider()));
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
    List<String> emails = coHosts(liveClass);
    if (!GoogleMeetLiveMeetingProvider.isSpaceName(liveClass.effectiveMeetingId())) {
      if (liveClass.getStatus() != LiveClassStatus.SCHEDULED) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "An active legacy Google meeting cannot change links;"
                + " add co-hosts in Google Meet instead");
      }
      User lead =
          liveClass.getLeadInstructor() != null
              ? liveClass.getLeadInstructor()
              : primaryInstructor(liveClass.getCourse());
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
  }

  private void checkHostLimit(List<String> emails) {
    if (emails.size() > 25) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Course teachers and manually added co-hosts cannot exceed 25 Google accounts");
    }
  }
}
