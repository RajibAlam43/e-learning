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

class PublicPlatformStatsApiIt extends AbstractPublicApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsOnlyPublicAndActivePlatformCounts() throws Exception {
    var activeStudent = user("Student One", "stats-student-one@example.com", UserStatus.ACTIVE);
    var secondStudent = user("Student Two", "stats-student-two@example.com", UserStatus.ACTIVE);
    var suspendedStudent =
        user("Suspended Student", "stats-student-suspended@example.com", UserStatus.SUSPENDED);
    assignRole(activeStudent, "STUDENT");
    assignRole(secondStudent, "STUDENT");
    assignRole(suspendedStudent, "STUDENT");

    var publicInstructor =
        user("Public Instructor", "stats-instructor-public@example.com", UserStatus.ACTIVE);
    var secondPublicInstructor =
        user("Public Instructor Two", "stats-instructor-two@example.com", UserStatus.ACTIVE);
    var privateInstructor =
        user("Private Instructor", "stats-instructor-private@example.com", UserStatus.ACTIVE);
    final var suspendedInstructor =
        user(
            "Suspended Instructor", "stats-instructor-suspended@example.com", UserStatus.SUSPENDED);
    instructorProfile(publicInstructor, true, "Public Instructor");
    instructorProfile(secondPublicInstructor, true, "Public Instructor Two");
    instructorProfile(privateInstructor, false, "Private Instructor");
    instructorProfile(suspendedInstructor, true, "Suspended Instructor");

    course(
        "Published One",
        uniqueSlug("stats-course-one"),
        PublishStatus.PUBLISHED,
        publicInstructor,
        CourseLevel.BEGINNER,
        CourseLanguage.EN,
        Instant.now());
    course(
        "Published Two",
        uniqueSlug("stats-course-two"),
        PublishStatus.PUBLISHED,
        publicInstructor,
        CourseLevel.INTERMEDIATE,
        CourseLanguage.BN,
        Instant.now());
    course(
        "Draft Course",
        uniqueSlug("stats-course-draft"),
        PublishStatus.DRAFT,
        publicInstructor,
        CourseLevel.ADVANCED,
        CourseLanguage.EN,
        null);

    collection(
        "Published Track",
        uniqueSlug("stats-track"),
        CollectionType.TRACK,
        PublishStatus.PUBLISHED,
        publicInstructor,
        Instant.now());
    collection(
        "Published Degree",
        uniqueSlug("stats-degree"),
        CollectionType.DEGREE,
        PublishStatus.PUBLISHED,
        publicInstructor,
        Instant.now());
    collection(
        "Draft Program",
        uniqueSlug("stats-draft"),
        CollectionType.DIPLOMA,
        PublishStatus.DRAFT,
        publicInstructor,
        null);

    mockMvc
        .perform(get("/public/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.students").value(2))
        .andExpect(jsonPath("$.courses").value(2))
        .andExpect(jsonPath("$.programs").value(2))
        .andExpect(jsonPath("$.instructors").value(2));
  }
}
