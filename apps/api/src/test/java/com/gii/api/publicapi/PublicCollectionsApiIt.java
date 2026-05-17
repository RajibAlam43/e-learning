package com.gii.api.publicapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.CollectionType;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class PublicCollectionsApiIt extends AbstractPublicApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void listsPublishedCollectionsAndAppliesTypeFilter() throws Exception {
    var creator = user("Creator", "creator-public-collections@example.com", UserStatus.ACTIVE);
    var instructor =
        user("Instructor", "inst-public-collections@example.com", UserStatus.ACTIVE);
    var c1 =
        course(
            "Track Course",
            uniqueSlug("track-course"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    attachInstructor(c1, instructor);
    var c2 =
        course(
            "Pack Course",
            uniqueSlug("pack-course"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    attachInstructor(c2, instructor);

    var track =
        collection(
            "Java Track",
            uniqueSlug("java-track"),
            CollectionType.TRACK,
            PublishStatus.PUBLISHED,
            creator,
            Instant.now());
    attachCourseToCollection(track, c1, 1, true);

    var pack =
        collection(
            "Toolkit Pack",
            uniqueSlug("toolkit-pack"),
            CollectionType.PACK,
            PublishStatus.PUBLISHED,
            creator,
            Instant.now());
    attachCourseToCollection(pack, c2, 1, true);

    collection(
        "Hidden Draft",
        uniqueSlug("hidden-draft"),
        CollectionType.TRACK,
        PublishStatus.DRAFT,
        creator,
        null);

    mockMvc
        .perform(get("/public/collections"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));

    mockMvc
        .perform(get("/public/collections").param("type", "TRACK"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].title").value("Java Track"));
  }

  @Test
  void collectionDetailsOmitsUnpublishedCourses() throws Exception {
    var creator = user("Creator", "creator-public-collections-2@example.com", UserStatus.ACTIVE);
    var published =
        course(
            "Published In Collection",
            uniqueSlug("published-in-collection"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    var draft =
        course(
            "Draft In Collection",
            uniqueSlug("draft-in-collection"),
            PublishStatus.DRAFT,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());

    var collection =
        collection(
            "Backend Pack",
            uniqueSlug("backend-pack"),
            CollectionType.PACK,
            PublishStatus.PUBLISHED,
            creator,
            Instant.now());
    attachCourseToCollection(collection, published, 1, true);
    attachCourseToCollection(collection, draft, 2, true);

    mockMvc
        .perform(get("/public/collections/{slug}", collection.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Backend Pack"))
        .andExpect(jsonPath("$.courseCount").value(1))
        .andExpect(jsonPath("$.courses.length()").value(1))
        .andExpect(jsonPath("$.courses[0].title").value("Published In Collection"));
  }
}
