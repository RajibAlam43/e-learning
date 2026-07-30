package com.gii.api.studentapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class StudentCollectionsApiIt extends AbstractStudentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void myCollectionsAndDetailsReturnProgress() throws Exception {
    var creator = user("Creator", "creator-student-collections@example.com");
    var student = user("Student", "student-collections@example.com");
    var collection = collection("Data Track", "data-track", creator, PublishStatus.PUBLISHED);

    var course1 = course("Course One", "course-one-stc", creator, PublishStatus.PUBLISHED);
    var course2 = course("Course Two", "course-two-stc", creator, PublishStatus.PUBLISHED);
    collectionCourse(collection, course1, 1, true);
    collectionCourse(collection, course2, 2, true);
    collectionEnrollment(student, collection, EnrollmentStatus.ACTIVE, null);

    var s1 = section(course1, 1, PublishStatus.PUBLISHED);
    var c1l1 = lesson(course1, s1, 1, PublishStatus.PUBLISHED, false);
    lesson(course1, s1, 2, PublishStatus.PUBLISHED, false);
    completedProgress(student, c1l1);

    var s2 = section(course2, 1, PublishStatus.PUBLISHED);
    lesson(course2, s2, 1, PublishStatus.PUBLISHED, false);

    mockMvc
        .perform(get("/student/collections").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].collectionName").value("Data Track"))
        .andExpect(jsonPath("$[0].courseCount").value(2))
        .andExpect(jsonPath("$[0].completedLessons").value(1))
        .andExpect(jsonPath("$[0].totalLessons").value(3))
        .andExpect(jsonPath("$[0].progressPercentage").value(33.33));

    mockMvc
        .perform(
            get("/student/collections/{collectionId}", collection.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.collectionName").value("Data Track"))
        .andExpect(jsonPath("$.progressPercentage").value(33.33))
        .andExpect(jsonPath("$.courses.length()").value(2))
        .andExpect(jsonPath("$.courses[0].completedLessons").value(1));
  }

  @Test
  void collectionDetailsReturnsNotFoundWhenNotEnrolled() throws Exception {
    var creator = user("Creator", "creator-student-collections-2@example.com");
    var student = user("Student", "student-collections-2@example.com");
    var collection = collection("Cloud Pack", "cloud-pack", creator, PublishStatus.PUBLISHED);

    mockMvc
        .perform(
            get("/student/collections/{collectionId}", collection.getId())
                .with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isNotFound());
  }
}
