package com.gii.api.publicapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class PublicCourseDetailsApiIt extends AbstractPublicApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsOnlyPublishedCourseDetailsAndPublishedContent() throws Exception {
    User creator = user("Creator", "creator4@example.com", UserStatus.ACTIVE);
    Course published =
        course(
            "Public Course",
            uniqueSlug("public-course"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    CourseSection publishedSection =
        section(published, uniqueSlug("sec-p"), 1, PublishStatus.PUBLISHED);
    section(published, uniqueSlug("sec-d"), 2, PublishStatus.DRAFT);
    Lesson freeLesson =
        lesson(
            published,
            publishedSection,
            uniqueSlug("lesson-free"),
            1,
            PublishStatus.PUBLISHED,
            true);
    lesson(
        published, publishedSection, uniqueSlug("lesson-paid"), 2, PublishStatus.PUBLISHED, false);
    lesson(published, publishedSection, uniqueSlug("lesson-draft"), 3, PublishStatus.DRAFT, true);
    mediaAsset(freeLesson, "yt123");

    mockMvc
        .perform(get("/public/courses/{slug}", published.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Public Course"))
        .andExpect(jsonPath("$.sections.length()").value(1))
        .andExpect(jsonPath("$.sections[0].lessons.length()").value(2))
        .andExpect(jsonPath("$.sections[0].lessons[0].video.sourceId").value("yt123"))
        .andExpect(jsonPath("$.sections[0].lessons[1].video").doesNotExist());
  }

  @Test
  void returns404ForUnknownOrUnpublishedCourse() throws Exception {
    User creator = user("Creator", "creator5@example.com", UserStatus.ACTIVE);
    Course draft =
        course(
            "Draft Course",
            uniqueSlug("draft-details"),
            PublishStatus.DRAFT,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            null);

    mockMvc.perform(get("/public/courses/{slug}", "missing-slug")).andExpect(status().isNotFound());
    mockMvc
        .perform(get("/public/courses/{slug}", draft.getSlug()))
        .andExpect(status().isNotFound());
  }

  @Test
  void includesCategoriesAndInstructors() throws Exception {
    User creator = user("Creator", "creator11@example.com", UserStatus.ACTIVE);
    User instructor = user("Instructor X", "ins-x@example.com", UserStatus.ACTIVE);
    Course course =
        course(
            "With Relations",
            uniqueSlug("with-rel"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    attachCategory(course, category("Programming", uniqueSlug("programming")));
    attachInstructor(course, instructor);

    mockMvc
        .perform(get("/public/courses/{slug}", course.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories.length()").value(1))
        .andExpect(jsonPath("$.categories[0].name").value("Programming"))
        .andExpect(jsonPath("$.instructors.length()").value(1))
        .andExpect(jsonPath("$.instructors[0].fullName").value("Instructor X"));
  }
}
