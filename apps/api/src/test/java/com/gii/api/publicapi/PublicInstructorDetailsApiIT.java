package com.gii.api.publicapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class PublicInstructorDetailsApiIT extends AbstractPublicApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsPublishedCoursesForActivePublicInstructor() throws Exception {
    User creator = user("Creator", "creator7@example.com", UserStatus.ACTIVE);
    User instructor = user("Instructor D", "insD@example.com", UserStatus.ACTIVE);
    instructorProfile(instructor, true, "D");

    Course published =
        course(
            "Published for Instructor",
            uniqueSlug("ins-detail-p"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    Course draft =
        course(
            "Draft for Instructor",
            uniqueSlug("ins-detail-d"),
            PublishStatus.DRAFT,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            null);
    attachInstructor(published, instructor);
    attachInstructor(draft, instructor);

    mockMvc
        .perform(get("/public/instructors/{slug}", instructor.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Instructor D"))
        .andExpect(jsonPath("$.publishedCourses.length()").value(1))
        .andExpect(jsonPath("$.publishedCourses[0].title").value("Published for Instructor"));
  }

  @Test
  void returns404ForInvalidOrUnknownSlug() throws Exception {
    mockMvc
        .perform(get("/public/instructors/{slug}", "not-a-uuid"))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/public/instructors/{slug}", "44f96db0-e4ea-4a58-9d3d-0faf6515fa9f"))
        .andExpect(status().isNotFound());
  }

  @Test
  void returns404ForInactiveOrPrivateInstructor() throws Exception {
    User inactive = user("Inactive Instructor", "inactive@example.com", UserStatus.SUSPENDED);
    User privateActive = user("Private Instructor", "private@example.com", UserStatus.ACTIVE);
    instructorProfile(inactive, true, "Inactive");
    instructorProfile(privateActive, false, "Private");

    mockMvc
        .perform(get("/public/instructors/{slug}", inactive.getId()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/public/instructors/{slug}", privateActive.getId()))
        .andExpect(status().isNotFound());
  }
}
