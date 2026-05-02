package com.gii.api.studentapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StudentLiveClassQueriesDataJpaTest extends AbstractStudentDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void upcomingAndRegistrantQueriesWork() {
    var student = user("Live Jpa Student", "live-jpa-student@example.com");
    var instructor = user("Live Jpa Instructor", "live-jpa-ins@example.com");
    var creator = user("Live Jpa Creator", "live-jpa-creator@example.com");
    var course = course("Live Jpa Course", "live-jpa-course", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED, false);
    enrollment(student, course, EnrollmentStatus.ACTIVE, null);
    var live =
        liveClass(
            course,
            sec,
            lesson,
            instructor,
            LiveClassStatus.SCHEDULED,
            Instant.now().plusSeconds(300),
            Instant.now().plusSeconds(3600),
            "https://meet.test/jpa");
    registrant(student, live, LiveClassRegistrantStatus.APPROVED);

    assertThat(
            liveClassRepository.findUpcomingByCourseIds(
                List.of(course.getId()),
                List.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE),
                Instant.now()))
        .hasSize(1);
    assertThat(
            liveClassRegistrantRepository.findByUserIdAndLiveClassIds(
                student.getId(), List.of(live.getId())))
        .hasSize(1);
  }
}
