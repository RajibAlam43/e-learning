package com.gii.api.studentapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StudentLearningStreakRepositoryDataJpaTest extends AbstractStudentDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void atomicallyMaintainsCurrentAndMaximumStreakAndIgnoresSameDayDuplicates() {
    var student = user("Streak Student", "streak-repository@example.com");
    LocalDate firstDay = LocalDate.of(2026, 8, 1);

    record(student.getId(), firstDay, 9);
    record(student.getId(), firstDay, 15);
    record(student.getId(), firstDay.plusDays(1), 10);
    record(student.getId(), firstDay.plusDays(2), 10);
    record(student.getId(), firstDay.plusDays(3), 10);
    record(student.getId(), firstDay.plusDays(7), 10);
    record(student.getId(), firstDay.plusDays(8), 10);
    record(student.getId(), firstDay.plusDays(9), 10);

    var streak = studentLearningStreakRepository.findById(student.getId()).orElseThrow();
    assertThat(streak.getCurrentStreak()).isEqualTo(3);
    assertThat(streak.getMaxStreak()).isEqualTo(4);
    assertThat(streak.getLastActivityDate()).isEqualTo(firstDay.plusDays(9));
    assertThat(streak.getLastActivityAt()).isEqualTo(at(firstDay.plusDays(9), 10));
  }

  private void record(java.util.UUID userId, LocalDate date, int hour) {
    studentLearningStreakRepository.recordActivity(userId, date, at(date, hour));
  }

  private static Instant at(LocalDate date, int hour) {
    return date.atTime(hour, 0).toInstant(ZoneOffset.UTC);
  }
}
