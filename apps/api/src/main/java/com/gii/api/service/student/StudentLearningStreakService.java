package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentLearningStreakResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.enrollment.StudentLearningStreak;
import com.gii.common.repository.enrollment.StudentLearningStreakRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentLearningStreakService {

  private final CurrentUserService currentUserService;
  private final StudentLearningStreakRepository studentLearningStreakRepository;
  private final StudentLearningStreakTrackerService studentLearningStreakTrackerService;

  public StudentLearningStreakResponse execute(Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    ZoneId zoneId = studentLearningStreakTrackerService.resolveZoneId(userId);
    LocalDate today = LocalDate.now(zoneId);

    return studentLearningStreakRepository
        .findById(userId)
        .map(streak -> toResponse(streak, today))
        .orElseGet(
            () ->
                StudentLearningStreakResponse.builder()
                    .currentStreak(0)
                    .maxStreak(0)
                    .lastActivityDate(null)
                    .lastActivityAt(null)
                    .build());
  }

  private StudentLearningStreakResponse toResponse(StudentLearningStreak streak, LocalDate today) {
    int currentStreak =
        streak.getLastActivityDate().isBefore(today.minusDays(1)) ? 0 : streak.getCurrentStreak();
    return StudentLearningStreakResponse.builder()
        .currentStreak(currentStreak)
        .maxStreak(streak.getMaxStreak())
        .lastActivityDate(streak.getLastActivityDate())
        .lastActivityAt(streak.getLastActivityAt())
        .build();
  }
}
