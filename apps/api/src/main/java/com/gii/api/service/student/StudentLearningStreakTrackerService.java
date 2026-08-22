package com.gii.api.service.student;

import com.gii.common.entity.user.UserProfile;
import com.gii.common.repository.enrollment.StudentLearningStreakRepository;
import com.gii.common.repository.user.UserProfileRepository;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentLearningStreakTrackerService {

  private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Dhaka");

  private final UserProfileRepository userProfileRepository;
  private final StudentLearningStreakRepository studentLearningStreakRepository;

  @Transactional
  public void recordActivity(UUID userId, Instant activityAt) {
    ZoneId zoneId = resolveZoneId(userId);
    studentLearningStreakRepository.recordActivity(
        userId, activityAt.atZone(zoneId).toLocalDate(), activityAt);
  }

  ZoneId resolveZoneId(UUID userId) {
    String timezone =
        userProfileRepository.findById(userId).map(UserProfile::getTimezone).orElse(null);
    if (timezone == null || timezone.isBlank()) {
      return DEFAULT_ZONE;
    }
    try {
      return ZoneId.of(timezone);
    } catch (DateTimeException ignored) {
      return DEFAULT_ZONE;
    }
  }
}
