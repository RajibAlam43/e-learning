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

class PublicInstructorsApiIt extends AbstractPublicApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsOnlyPublicActiveInstructorsWithPublishedCourseCount() throws Exception {
    final User creator = user("Creator", "creator6@example.com", UserStatus.ACTIVE);
    User activePublic = user("Instructor A", "insA@example.com", UserStatus.ACTIVE);
    User suspendedPublic = user("Instructor B", "insB@example.com", UserStatus.SUSPENDED);
    User activePrivate = user("Instructor C", "insC@example.com", UserStatus.ACTIVE);

    instructorProfile(activePublic, true, "A");
    instructorProfile(suspendedPublic, true, "B");
    instructorProfile(activePrivate, false, "C");

    Course published =
        course(
            "Published",
            uniqueSlug("ins-course-p"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    Course draft =
        course(
            "Draft",
            uniqueSlug("ins-course-d"),
            PublishStatus.DRAFT,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            null);
    attachInstructor(published, activePublic);
    attachInstructor(draft, activePublic);

    mockMvc
        .perform(get("/public/instructors"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].fullName").value("Instructor A"))
        .andExpect(jsonPath("$[0].publishedCoursesCount").value(1));
  }

  @Test
  void returnsEmptyListWhenNoPublicActiveInstructors() throws Exception {
    mockMvc
        .perform(get("/public/instructors"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
