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

class PublicCoursesApiIT extends AbstractPublicApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsOnlyPublishedCourses() throws Exception {
    User creator = user("Creator", "creator1@example.com", UserStatus.ACTIVE);
    course(
        "Published Course",
        uniqueSlug("published-course"),
        PublishStatus.PUBLISHED,
        creator,
        CourseLevel.BEGINNER,
        CourseLanguage.EN,
        Instant.now());
    course(
        "Draft Course",
        uniqueSlug("draft-course"),
        PublishStatus.DRAFT,
        creator,
        CourseLevel.BEGINNER,
        CourseLanguage.EN,
        Instant.now());

    mockMvc
        .perform(get("/public/courses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].title").value("Published Course"));
  }

  @Test
  void filtersByCategoryLevelAndLanguage() throws Exception {
    User creator = user("Creator", "creator2@example.com", UserStatus.ACTIVE);
    Course courseA =
        course(
            "Java EN Beginner",
            uniqueSlug("java-en"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    Course courseB =
        course(
            "Bangla Advanced",
            uniqueSlug("bn-adv"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.ADVANCED,
            CourseLanguage.BN,
            Instant.now());

    var category = category("Programming", uniqueSlug("programming"));
    attachCategory(courseA, category);
    attachCategory(courseB, category("Business", uniqueSlug("business")));

    mockMvc
        .perform(
            get("/public/courses")
                .param("categoryId", category.getId().toString())
                .param("level", "BEGINNER")
                .param("language", "EN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].title").value("Java EN Beginner"));
  }

  @Test
  void enforcesMaxPageSize() throws Exception {
    User creator = user("Creator", "creator3@example.com", UserStatus.ACTIVE);
    for (int i = 0; i < 25; i++) {
      course(
          "Course " + i,
          uniqueSlug("course-" + i),
          PublishStatus.PUBLISHED,
          creator,
          CourseLevel.BEGINNER,
          CourseLanguage.EN,
          Instant.now().minusSeconds(i));
    }

    mockMvc
        .perform(get("/public/courses").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.content.length()").value(20));
  }

  @Test
  void returnsEmptyPageWhenNoMatch() throws Exception {
    User creator = user("Creator", "creator-empty@example.com", UserStatus.ACTIVE);
    course(
        "Only BN Course",
        uniqueSlug("only-bn"),
        PublishStatus.PUBLISHED,
        creator,
        CourseLevel.BEGINNER,
        CourseLanguage.BN,
        Instant.now());

    mockMvc
        .perform(get("/public/courses").param("language", "EN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void rejectsInvalidEnumAndUuidFilters() throws Exception {
    mockMvc
        .perform(get("/public/courses").param("level", "INVALID_LEVEL"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(get("/public/courses").param("categoryId", "not-a-uuid"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void defaultsToStablePublishedSortWhenSortMissing() throws Exception {
    User creator = user("Creator", "creator-sort@example.com", UserStatus.ACTIVE);
    course(
        "Older",
        uniqueSlug("older"),
        PublishStatus.PUBLISHED,
        creator,
        CourseLevel.BEGINNER,
        CourseLanguage.EN,
        Instant.parse("2025-01-01T00:00:00Z"));
    course(
        "Newer",
        uniqueSlug("newer"),
        PublishStatus.PUBLISHED,
        creator,
        CourseLevel.BEGINNER,
        CourseLanguage.EN,
        Instant.parse("2025-02-01T00:00:00Z"));

    mockMvc
        .perform(get("/public/courses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("Older"))
        .andExpect(jsonPath("$.content[1].title").value("Newer"));
  }

  @Test
  void ignoresUnsafeSortFieldAndFallsBackToDefault() throws Exception {
    User creator = user("Creator", "creator-unsafe-sort@example.com", UserStatus.ACTIVE);
    course(
        "Older2",
        uniqueSlug("older2"),
        PublishStatus.PUBLISHED,
        creator,
        CourseLevel.BEGINNER,
        CourseLanguage.EN,
        Instant.parse("2025-01-01T00:00:00Z"));
    course(
        "Newer2",
        uniqueSlug("newer2"),
        PublishStatus.PUBLISHED,
        creator,
        CourseLevel.BEGINNER,
        CourseLanguage.EN,
        Instant.parse("2025-02-01T00:00:00Z"));

    mockMvc
        .perform(get("/public/courses").param("sort", "createdBy,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("Newer2"));
  }
}
