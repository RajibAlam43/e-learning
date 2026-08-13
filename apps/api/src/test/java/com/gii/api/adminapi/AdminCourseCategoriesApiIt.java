package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class AdminCourseCategoriesApiIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void assignsRequiredCategoriesOnCreateAndReplacesThemOnUpdate() throws Exception {
    var admin = user("Course Category Admin", "course-category-admin@example.com");
    var programming = category("প্রোগ্রামিং", "Programming", "programming");
    var technology = category("প্রযুক্তি", "Technology", "technology");
    var business = category("ব্যবসা", "Business", "business");

    mockMvc
        .perform(
            post("/admin/courses")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Course Categories",
                      "slug":"course-categories",
                      "categoryIds":["%s","%s"],
                      "priceBdt":1000,
                      "level":"BEGINNER",
                      "language":"BN",
                      "studyMode":"SELF_PACED"
                    }
                    """
                        .formatted(programming.getId(), technology.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories.length()").value(2));

    var course = courseRepository.findAll().getFirst();
    assertThat(courseCategoryRepository.findByCourseId(course.getId())).hasSize(2);

    mockMvc
        .perform(
            patch("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryIds\":[\"%s\"]}".formatted(business.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories.length()").value(1))
        .andExpect(jsonPath("$.categories[0].id").value(business.getId().toString()));
  }

  @Test
  void rejectsMissingEmptyDuplicateAndUnknownCategories() throws Exception {
    var admin = user("Course Category Validator", "course-category-validator@example.com");
    var category = category("প্রযুক্তি", "Technology", "technology");
    String requiredCourseFields =
        "\"title\":\"Invalid Course\",\"slug\":\"invalid-course\",\"priceBdt\":1000,"
            + "\"level\":\"BEGINNER\",\"language\":\"BN\",\"studyMode\":\"SELF_PACED\"";

    assertInvalidCourse(admin.getId(), "{" + requiredCourseFields + "}");
    assertInvalidCourse(admin.getId(), "{" + requiredCourseFields + ",\"categoryIds\":[]}");
    assertInvalidCourse(
        admin.getId(),
        "{"
            + requiredCourseFields
            + ",\"categoryIds\":[\""
            + category.getId()
            + "\",\""
            + category.getId()
            + "\"]}");
    assertInvalidCourse(
        admin.getId(),
        "{" + requiredCourseFields + ",\"categoryIds\":[\"" + UUID.randomUUID() + "\"]}");
    assertThat(courseRepository.findAll()).isEmpty();
  }

  private void assertInvalidCourse(UUID adminId, String body) throws Exception {
    mockMvc
        .perform(
            post("/admin/courses")
                .with(authentication(adminAuth(adminId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }
}
