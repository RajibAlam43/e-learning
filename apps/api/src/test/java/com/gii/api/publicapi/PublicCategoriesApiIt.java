package com.gii.api.publicapi;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class PublicCategoriesApiIt extends AbstractPublicApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void listsCategoriesUsingRequestedLanguage() throws Exception {
    var parent = category("প্রযুক্তি", "technology");
    parent.setNameEn("Technology");
    categoryRepository.save(parent);
    var child = category("প্রোগ্রামিং", "programming");
    child.setNameEn("Programming");
    child.setParent(parent);
    categoryRepository.save(child);

    mockMvc
        .perform(get("/public/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.slug == 'technology')].name", hasItem("প্রযুক্তি")))
        .andExpect(jsonPath("$[?(@.slug == 'programming')].name", hasItem("প্রোগ্রামিং")))
        .andExpect(
            jsonPath("$[?(@.slug == 'programming')].parentId", hasItem(parent.getId().toString())));

    mockMvc
        .perform(get("/public/categories").param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Programming"))
        .andExpect(jsonPath("$[1].name").value("Technology"));
  }
}
