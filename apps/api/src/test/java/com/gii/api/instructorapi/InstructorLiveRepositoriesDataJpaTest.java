package com.gii.api.instructorapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InstructorLiveRepositoriesDataJpaTest extends AbstractInstructorDataJpaTest {

  @AfterEach
  void cleanup() {
    cleanupInstructorData();
  }

  @Test
  void liveClassRegistrantAndAttendanceQueriesSupportInstructorFlows() {
    var creator = user("Creator", "creator-inst-jpa1@example.com");
    var instructor = user("Instructor", "inst-jpa1@example.com");
    var student = user("Student", "student-inst-jpa1@example.com");
    var course = course("Course JPA 1", "course-jpa-inst-1", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    var live =
        liveClass(
            course,
            sec,
            lesson,
            instructor,
            LiveClassStatus.SCHEDULED,
            Instant.now().plusSeconds(1800),
            Instant.now().plusSeconds(3600));
    registrant(student, live, LiveClassRegistrantStatus.APPROVED);
    attendance(student, live);

    assertThat(liveClassRepository.findByIdAndInstructorId(live.getId(), instructor.getId()))
        .isPresent();
    assertThat(
            liveClassRepository.findUpcomingByCourseIds(
                List.of(course.getId()),
                List.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE),
                Instant.now()))
        .hasSize(1);
    assertThat(
            liveClassRegistrantRepository.countByLiveClassIdAndStatus(
                live.getId(), LiveClassRegistrantStatus.APPROVED))
        .isEqualTo(1);
    assertThat(liveClassRegistrantRepository.findByLiveClassIdOrderByCreatedAtAsc(live.getId()))
        .hasSize(1);
    assertThat(liveClassAttendanceRepository.findByLiveClassId(live.getId())).hasSize(1);
  }
}
