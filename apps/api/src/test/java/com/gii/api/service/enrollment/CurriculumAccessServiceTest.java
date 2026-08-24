package com.gii.api.service.enrollment;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CurriculumAccessServiceTest {

  private final CurriculumAccessService service = new CurriculumAccessService();

  @Test
  void requiresPublishedReleasedSectionAndUnexpiredEnrollment() {
    Instant now = Instant.parse("2026-08-23T12:00:00Z");
    Enrollment enrollment = Enrollment.builder().enrolledAt(now.minusSeconds(2 * 86400L)).build();
    CourseSection section =
        CourseSection.builder()
            .status(PublishStatus.DRAFT)
            .releaseType(ReleaseType.IMMEDIATE)
            .build();

    assertThat(service.isSectionAccessible(section, enrollment, now)).isFalse();

    section.setStatus(PublishStatus.PUBLISHED);
    section.setReleaseType(ReleaseType.RELATIVE_DAYS);
    section.setUnlockAfterDays(3);
    assertThat(service.isSectionAccessible(section, enrollment, now)).isFalse();

    section.setUnlockAfterDays(2);
    assertThat(service.isSectionAccessible(section, enrollment, now)).isTrue();

    enrollment.setExpiresAt(now);
    assertThat(service.isSectionAccessible(section, enrollment, now)).isFalse();
  }
}
