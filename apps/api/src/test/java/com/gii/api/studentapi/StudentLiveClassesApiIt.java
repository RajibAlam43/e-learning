package com.gii.api.studentapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

class StudentLiveClassesApiIt extends AbstractStudentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void upcomingAndCourseLiveClassesReturnEnrolledData() throws Exception {
    var student = user("Student Six", "student6@example.com");
    var instructor = user("Instructor Six", "instructor6@example.com");
    var creator = user("Creator", "creator-stu6@example.com");
    var course = course("Course Live", "course-live", creator, PublishStatus.PUBLISHED);
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
            Instant.now().plusSeconds(1200),
            Instant.now().plusSeconds(3600),
            "https://meet.test/live");
    registrant(student, live, LiveClassRegistrantStatus.APPROVED);

    mockMvc
        .perform(get("/student/live-classes").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].courseName").value("Course Live"));

    mockMvc
        .perform(
            get("/student/courses/{courseId}/live-classes", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].liveClassId").value(live.getId().toString()));
  }

  @Test
  void joinLiveClassEnforcesGates() throws Exception {
    var student = user("Student Seven", "student7@example.com");
    var instructor = user("Instructor Seven", "instructor7@example.com");
    var creator = user("Creator", "creator-stu7@example.com");
    var course = course("Course Join", "course-join", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED, false);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(7200));
    var live =
        liveClass(
            course,
            sec,
            lesson,
            instructor,
            LiveClassStatus.LIVE,
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(1800),
            "https://meet.test/join");
    registrant(student, live, LiveClassRegistrantStatus.APPROVED);

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/join", live.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.joinUrl").value("https://zoom.test/join/" + live.getId()));

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/join", java.util.UUID.randomUUID())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isNotFound());
  }

  @Test
  void joinLiveClassRejectsWhenStatusIsNotLiveEvenInsideWindow() throws Exception {
    var student = user("Student Eight", "student8@example.com");
    var instructor = user("Instructor Eight", "instructor8@example.com");
    var creator = user("Creator", "creator-stu8@example.com");
    var course = course("Course Join 2", "course-join-2", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED, false);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(7200));
    var scheduled =
        liveClass(
            course,
            sec,
            lesson,
            instructor,
            LiveClassStatus.SCHEDULED,
            Instant.now().plusSeconds(120),
            Instant.now().plusSeconds(1800),
            "https://meet.test/join2");
    registrant(student, scheduled, LiveClassRegistrantStatus.APPROVED);

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/join", scheduled.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isForbidden());
  }

  @Test
  void joinLiveClassAutoRegistersEnrolledStudentWhenMissingRegistrant() throws Exception {
    var student = user("Student Nine", "student9@example.com");
    var instructor = user("Instructor Nine", "instructor9@example.com");
    var creator = user("Creator", "creator-stu9@example.com");
    var course = course("Course Join 3", "course-join-3", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED, false);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(7200));
    var live =
        liveClass(
            course,
            sec,
            lesson,
            instructor,
            LiveClassStatus.LIVE,
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(1800),
            "https://meet.test/join3");

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/join", live.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.joinUrl").value("https://meet.test/join3"));
  }

  @Test
  void joinLiveClassRejectsWhenCapacityIsFull() throws Exception {
    var studentA = user("Student Ten A", "student10a@example.com");
    var studentB = user("Student Ten B", "student10b@example.com");
    var instructor = user("Instructor Ten", "instructor10@example.com");
    var creator = user("Creator", "creator-stu10@example.com");
    var course = course("Course Join 4", "course-join-4", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED, false);
    enrollment(studentA, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(7200));
    enrollment(studentB, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(7200));
    var live =
        liveClass(
            course,
            sec,
            lesson,
            instructor,
            LiveClassStatus.LIVE,
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(1800),
            "https://meet.test/join4");
    live.setMaxCapacity(1);
    liveClassRepository.save(live);

    registrant(studentA, live, LiveClassRegistrantStatus.APPROVED);

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/join", live.getId())
                .with(authentication(studentAuth(studentB.getId()))))
        .andExpect(status().isForbidden());
  }

  @Test
  void joinLiveClassRejectsInstructorRoleOnSharedEndpoint() throws Exception {
    var student = user("Student Eleven", "student11@example.com");
    var instructor = user("Instructor Eleven", "instructor11@example.com");
    var creator = user("Creator", "creator-stu11@example.com");
    var course = course("Course Join 5", "course-join-5", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED, false);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().plusSeconds(7200));
    var live =
        liveClass(
            course,
            sec,
            lesson,
            instructor,
            LiveClassStatus.LIVE,
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(1800),
            "https://meet.test/join5");

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/join", live.getId())
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isForbidden());
  }

  private Authentication instructorAuth(java.util.UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")));
  }
}
