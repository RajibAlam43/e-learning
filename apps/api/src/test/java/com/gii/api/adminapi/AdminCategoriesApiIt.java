package com.gii.api.adminapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class AdminCategoriesApiIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void createsListsAndUpdatesBilingualCategories() throws Exception {
    var admin = user("Category Admin", "category-admin@example.com");
    var parent = category("প্রযুক্তি", "Technology", "technology");

    mockMvc
        .perform(
            post("/admin/categories")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name":"প্রোগ্রামিং",
                      "nameEn":"Programming",
                      "slug":"programming",
                      "parentId":"%s"
                    }
                    """
                        .formatted(parent.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("প্রোগ্রামিং"))
        .andExpect(jsonPath("$.nameEn").value("Programming"))
        .andExpect(jsonPath("$.parentId").value(parent.getId().toString()));

    var created = categoryRepository.findBySlug("programming").orElseThrow();
    mockMvc
        .perform(
            patch("/admin/categories/{categoryId}", created.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nameEn\":\"Software Development\",\"slug\":\"software-development\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nameEn").value("Software Development"))
        .andExpect(jsonPath("$.slug").value("software-development"));

    mockMvc
        .perform(get("/admin/categories").with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void explicitNullClearsParentWhileOmittedParentPreservesIt() throws Exception {
    var admin = user("Category Parent Admin", "category-parent-admin@example.com");
    var parent = category("প্রযুক্তি", "Technology", "technology");
    var child = category("প্রোগ্রামিং", "Programming", "programming");
    child.setParent(parent);
    categoryRepository.save(child);

    mockMvc
        .perform(
            patch("/admin/categories/{categoryId}", child.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nameEn\":\"Software Development\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentId").value(parent.getId().toString()));

    mockMvc
        .perform(
            patch("/admin/categories/{categoryId}", child.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentId\":null}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentId").doesNotExist());

    org.assertj.core.api.Assertions.assertThat(
            categoryRepository.findById(child.getId()).orElseThrow().getParent())
        .isNull();
  }

  @Test
  void requiresBothNamesAndUniqueSlug() throws Exception {
    var admin = user("Category Validator", "category-validator@example.com");
    category("প্রযুক্তি", "Technology", "technology");

    mockMvc
        .perform(
            post("/admin/categories")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ব্যবসা\",\"slug\":\"business\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/admin/categories")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"অন্য\",\"nameEn\":\"Other\",\"slug\":\"technology\"}"))
        .andExpect(status().isConflict());
  }
}
