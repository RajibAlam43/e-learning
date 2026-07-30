package com.gii.api.instructorapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class InstructorDashboardApiIt extends AbstractInstructorApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupInstructorData();
  }

  @Test
  void dashboardReturnsAssignedCoursesAndUpcomingLiveClasses() throws Exception {
    var creator = user("Creator", "creator-inst-dash@example.com");
    var instructor = user("Instructor One", "inst-dash@example.com");
    var studentA = user("Student A", "student-a-inst-dash@example.com");
    var studentB = user("Student B", "student-b-inst-dash@example.com");
    var course = course("Instructor Course", "instructor-course", creator, PublishStatus.PUBLISHED);
    assignment(course, instructor, InstructorRole.PRIMARY);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var lesson = lesson(course, sec, 1, PublishStatus.PUBLISHED);
    enrollment(studentA, course, EnrollmentStatus.ACTIVE, null);
    enrollment(studentB, course, EnrollmentStatus.ACTIVE, null);
    var upcoming =
        liveClass(
            course,
            sec,
            lesson,
            instructor,
            LiveClassStatus.SCHEDULED,
            Instant.now().plusSeconds(1800),
            Instant.now().plusSeconds(3600));
    course.setTitleEn("Instructor Course English");
    courseRepository.saveAndFlush(course);
    sec.setTitleEn("Instructor Section English");
    courseSectionRepository.saveAndFlush(sec);
    upcoming.setTitleEn("Upcoming Class English");
    upcoming.setDescriptionEn("Upcoming description English");
    liveClassRepository.saveAndFlush(upcoming);
    registrant(studentA, upcoming, LiveClassRegistrantStatus.APPROVED);
    registrant(studentB, upcoming, LiveClassRegistrantStatus.PENDING);

    mockMvc
        .perform(
            get("/instructor/dashboard").with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Instructor One"))
        .andExpect(jsonPath("$.totalCoursesAssigned").value(1))
        .andExpect(jsonPath("$.activeCourses").value(1))
        .andExpect(jsonPath("$.totalStudentsAcrossAllCourses").value(2))
        .andExpect(jsonPath("$.assignedCourses[0].courseSlug").value("instructor-course"))
        .andExpect(jsonPath("$.upcomingLiveClasses.length()").value(1))
        .andExpect(jsonPath("$.upcomingLiveClasses[0].registeredStudents").value(1))
        .andExpect(jsonPath("$.upcomingLiveClasses[0].timeLabel").isString())
        .andExpect(jsonPath("$.upcomingLiveClasses[0].timeLabel").isNotEmpty());

    mockMvc
        .perform(
            get("/instructor/dashboard")
                .param("lang", "en")
                .with(authentication(instructorAuth(instructor.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assignedCourses[0].courseName").value("Instructor Course English"))
        .andExpect(jsonPath("$.upcomingLiveClasses[0].title").value("Upcoming Class English"))
        .andExpect(
            jsonPath("$.upcomingLiveClasses[0].description")
                .value("Upcoming description English"))
        .andExpect(
            jsonPath("$.upcomingLiveClasses[0].sectionTitle")
                .value("Instructor Section English"));
  }
}
