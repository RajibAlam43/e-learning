package com.gii.api.studentapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.api.service.student.StudentLearningStreakTrackerService;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class StudentLearningStatsApiIt extends AbstractStudentApiIntegrationTest {

  private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

  @Autowired private MockMvc mockMvc;
  @Autowired private StudentLearningStreakTrackerService studentLearningStreakTrackerService;

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void returnsLessonOnlyTotalAndPersistedStreakData() throws Exception {
    final LocalDate today = LocalDate.now(TEST_ZONE);
    var student = user("Stats Student", "learning-stats@example.com");
    var studentProfile = profile(student, null);
    studentProfile.setTimezone(TEST_ZONE.getId());
    userProfileRepository.save(studentProfile);
    var creator = user("Stats Creator", "learning-stats-creator@example.com");
    var course = course("Stats Course", "stats-course", creator, PublishStatus.PUBLISHED);
    var section = section(course, 1, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, null);

    for (int i = 1; i <= 5; i++) {
      var lesson = lesson(course, section, i, PublishStatus.PUBLISHED, false);
      completedProgress(student, lesson);
    }

    studentLearningStreakTrackerService.recordActivity(student.getId(), atDay(today, -9));
    studentLearningStreakTrackerService.recordActivity(student.getId(), atDay(today, -8));
    studentLearningStreakTrackerService.recordActivity(student.getId(), atDay(today, -7));
    studentLearningStreakTrackerService.recordActivity(student.getId(), atDay(today, -6));
    studentLearningStreakTrackerService.recordActivity(student.getId(), atDay(today, -2));
    studentLearningStreakTrackerService.recordActivity(student.getId(), atDay(today, -1));
    studentLearningStreakTrackerService.recordActivity(student.getId(), atDay(today, 0));

    mockMvc
        .perform(
            get("/student/progress/completed-lessons")
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCompletedLessons").value(5));

    mockMvc
        .perform(get("/student/progress/streak").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentStreak").value(3))
        .andExpect(jsonPath("$.maxStreak").value(4))
        .andExpect(jsonPath("$.lastActivityDate").value(today.toString()))
        .andExpect(jsonPath("$.lastActivityAt").value(atDay(today, 0).toString()));
  }

  @Test
  void returnsZeroStatsForStudentWithoutLearningActivity() throws Exception {
    var student = user("New Student", "new-learning-stats@example.com");

    mockMvc
        .perform(
            get("/student/progress/completed-lessons")
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCompletedLessons").value(0));

    mockMvc
        .perform(get("/student/progress/streak").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentStreak").value(0))
        .andExpect(jsonPath("$.maxStreak").value(0))
        .andExpect(jsonPath("$.lastActivityDate").doesNotExist())
        .andExpect(jsonPath("$.lastActivityAt").doesNotExist());
  }

  private static Instant atDay(LocalDate today, long daysFromToday) {
    return today.plusDays(daysFromToday).atTime(12, 0).atZone(TEST_ZONE).toInstant();
  }
}
