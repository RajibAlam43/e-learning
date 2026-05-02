package com.gii.api.studentapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class StudentCoursesApiIt extends AbstractStudentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void getMyCoursesReturnsOnlyActiveEnrollments() throws Exception {
    var student = user("Student Two", "student2@example.com");
    var creator = user("Creator", "creator-stu2@example.com");
    var activeCourse = course("Active Course", "active-course", creator, PublishStatus.PUBLISHED);
    var revokedCourse =
        course("Revoked Course", "revoked-course", creator, PublishStatus.PUBLISHED);
    var s1 = section(activeCourse, 1, PublishStatus.PUBLISHED);
    var l1 = lesson(activeCourse, s1, 1, PublishStatus.PUBLISHED, false);
    enrollment(student, activeCourse, EnrollmentStatus.ACTIVE, null);
    enrollment(student, revokedCourse, EnrollmentStatus.REVOKED, null);
    completedProgress(student, l1);

    mockMvc
        .perform(get("/student/courses").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].courseName").value("Active Course"))
        .andExpect(jsonPath("$[0].completedLessons").value(1))
        .andExpect(jsonPath("$[0].totalLessons").value(1));
  }

  @Test
  void getMyCourseDetailsReturns404ForNotEnrolledCourse() throws Exception {
    var student = user("Student Three", "student3@example.com");
    var creator = user("Creator", "creator-stu3@example.com");
    var course = course("Not Enrolled", "not-enrolled", creator, PublishStatus.PUBLISHED);

    mockMvc
        .perform(
            get("/student/courses/{courseId}", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isNotFound());
  }

  @Test
  void getMyCourseDetailsReturnsPublishedSectionsAndLessons() throws Exception {
    var student = user("Student Four", "student4@example.com");
    var creator = user("Creator", "creator-stu4@example.com");
    var course = course("Course Home", "course-home", creator, PublishStatus.PUBLISHED);
    var sec = section(course, 1, PublishStatus.PUBLISHED);
    var l1 = lesson(course, sec, 1, PublishStatus.PUBLISHED, true);
    lesson(course, sec, 2, PublishStatus.PUBLISHED, false);
    enrollment(student, course, EnrollmentStatus.ACTIVE, null);
    completedProgress(student, l1);

    mockMvc
        .perform(
            get("/student/courses/{courseId}", course.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.courseName").value("Course Home"))
        .andExpect(jsonPath("$.sections.length()").value(1))
        .andExpect(jsonPath("$.sections[0].lessons.length()").value(2))
        .andExpect(jsonPath("$.sections[0].completedLessons").value(1));
  }
}
