package com.gii.api.meapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.EnrollmentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MeStatsRepositoriesDataJpaTest extends AbstractMeDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupMeData();
  }

  @Test
  void statsRepositoriesReturnExpectedCountsForMeAggregates() {
    var user = user("Stats User", "stats-user@example.com", "+8801710000201");
    var creator = user("Creator", "creator-stats@example.com", "+8801710000202");
    var course1 = course("Stats C1", "stats-c1", creator);
    var course2 = course("Stats C2", "stats-c2", creator);
    enrollment(user, course1, true);
    enrollment(user, course2, false);
    certificate(user, course1, "GII-CERT-MESTATS1", false, creator);
    certificate(user, course2, "GII-CERT-MESTATS2", true, creator);
    var sec = section(course1, 1);
    var les = lesson(course1, sec, 1);
    var lc = liveClass(course1, sec, les, creator);
    attendance(user, lc);

    assertThat(enrollmentRepository.countByUserIdAndStatus(user.getId(), EnrollmentStatus.ACTIVE))
        .isEqualTo(2);
    assertThat(
            enrollmentRepository.countByUserIdAndStatusAndCompletedAtIsNotNull(
                user.getId(), EnrollmentStatus.ACTIVE))
        .isEqualTo(1);
    assertThat(certificateRepository.countByUserIdAndRevokedAtIsNull(user.getId())).isEqualTo(1);
    assertThat(liveClassAttendanceRepository.countByUserId(user.getId())).isEqualTo(1);
  }
}
