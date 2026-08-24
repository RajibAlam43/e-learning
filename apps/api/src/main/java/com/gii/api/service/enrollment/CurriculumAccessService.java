package com.gii.api.service.enrollment;

import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurriculumAccessService {

  public boolean isEnrollmentExpired(Enrollment enrollment, Instant now) {
    return enrollment.getExpiresAt() != null && !enrollment.getExpiresAt().isAfter(now);
  }

  public boolean isSectionAccessible(CourseSection section, Enrollment enrollment, Instant now) {
    return section.getStatus() == PublishStatus.PUBLISHED
        && !isEnrollmentExpired(enrollment, now)
        && isReleased(
            section.getReleaseType(),
            section.getReleaseAt(),
            section.getUnlockAfterDays(),
            enrollment,
            now);
  }

  public boolean isReleased(
      ReleaseType releaseType,
      Instant releaseAt,
      Integer unlockAfterDays,
      Enrollment enrollment,
      Instant now) {
    if (releaseType == null || releaseType == ReleaseType.IMMEDIATE) {
      return true;
    }
    if (releaseType == ReleaseType.FIXED_DATE) {
      return releaseAt == null || !now.isBefore(releaseAt);
    }
    if (releaseType == ReleaseType.RELATIVE_DAYS) {
      if (unlockAfterDays == null) {
        return true;
      }
      return !now.isBefore(enrollment.getEnrolledAt().plusSeconds(unlockAfterDays * 86400L));
    }
    return true;
  }

  public void requireSectionAccess(
      CourseSection section, Enrollment enrollment, Instant now, String resourceName) {
    if (section.getStatus() != PublishStatus.PUBLISHED) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, resourceName + " not found");
    }
    if (isEnrollmentExpired(enrollment, now)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment expired");
    }
    if (!isSectionAccessible(section, enrollment, now)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Section is not available yet");
    }
  }
}
