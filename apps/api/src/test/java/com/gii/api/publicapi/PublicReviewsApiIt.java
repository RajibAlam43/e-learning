package com.gii.api.publicapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReviewStatus;
import com.gii.common.enums.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class PublicReviewsApiIt extends AbstractPublicApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void listsOnlyPublishedReviewsForPublishedCoursesAndFiltersByRating() throws Exception {
    var creator = user("Creator", "all-reviews-creator@example.com", UserStatus.ACTIVE);
    var firstStudent = user("First Student", "all-reviews-first@example.com", UserStatus.ACTIVE);
    var secondStudent = user("Second Student", "all-reviews-second@example.com", UserStatus.ACTIVE);
    var publishedCourse =
        course(
            "Published Course",
            uniqueSlug("all-reviews-published"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    var draftCourse =
        course(
            "Draft Course",
            uniqueSlug("all-reviews-draft"),
            PublishStatus.DRAFT,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            null);

    review(publishedCourse, firstStudent, 5, "Five stars", ReviewStatus.PUBLISHED);
    review(publishedCourse, secondStudent, 4, "Four stars", ReviewStatus.PUBLISHED);
    review(publishedCourse, creator, 5, "Pending", ReviewStatus.PENDING);
    review(draftCourse, firstStudent, 5, "Draft course", ReviewStatus.PUBLISHED);

    mockMvc
        .perform(get("/public/reviews").param("rating", "5").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].reviewText").value("Five stars"))
        .andExpect(jsonPath("$.content[0].rating").value(5))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(1));

    mockMvc
        .perform(get("/public/reviews"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void rejectsRatingOutsideStarRange() throws Exception {
    mockMvc.perform(get("/public/reviews").param("rating", "0")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/public/reviews").param("rating", "6")).andExpect(status().isBadRequest());
  }
}
