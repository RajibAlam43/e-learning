package com.gii.api.studentapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReviewStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

class StudentCourseReviewsApiIt extends AbstractStudentApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupStudentData();
  }

  @Test
  void reviewRequiresEnrollmentAndModerationBeforePublicVisibility() throws Exception {
    var creator = user("Creator", "review-creator@example.com");
    var student = user("Student", "review-student@example.com");
    var course = course("Review Course", "review-course", creator, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, null);

    String response =
        mockMvc
            .perform(
                post("/student/courses/{courseId}/reviews", course.getId())
                    .with(authentication(studentAuth(student.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"rating":5,"reviewText":"  Excellent course  "}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.reviewText").value("Excellent course"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    var review = courseReviewRepository.findAll().getFirst();
    assertThat(response).contains(review.getId().toString());
    assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING);

    mockMvc
        .perform(get("/public/courses/{slug}/reviews", course.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    mockMvc
        .perform(get("/admin/reviews").with(authentication(adminAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("PENDING"));

    mockMvc
        .perform(
            post("/admin/reviews/{reviewId}/publish", review.getId())
                .with(authentication(adminAuthentication())))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/public/courses/{slug}/reviews", course.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].studentName").value("Student"))
        .andExpect(jsonPath("$[0].rating").value(5));

    mockMvc
        .perform(get("/public/courses/{slug}", course.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.averageRating").value(5.0))
        .andExpect(jsonPath("$.totalReviews").value(1));

    mockMvc
        .perform(
            get("/admin/reviews")
                .queryParam("status", "PUBLISHED")
                .with(authentication(adminAuthentication())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].reviewId").value(review.getId().toString()))
        .andExpect(jsonPath("$[0].studentId").value(student.getId().toString()));

    mockMvc
        .perform(
            post("/student/courses/{courseId}/reviews", course.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"rating":4,"reviewText":"Second review"}
                    """))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            post("/admin/reviews/{reviewId}/unpublish", review.getId())
                .with(authentication(adminAuthentication())))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/public/courses/{slug}/reviews", course.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    mockMvc
        .perform(
            delete("/admin/reviews/{reviewId}", review.getId())
                .with(authentication(adminAuthentication())))
        .andExpect(status().isNoContent());
    assertThat(courseReviewRepository.existsById(review.getId())).isFalse();
  }

  @Test
  void reviewRejectsUnauthorizedInvalidNonEnrolledAndExpiredRequests() throws Exception {
    var creator = user("Creator", "review-edge-creator@example.com");
    var student = user("Student", "review-edge-student@example.com");
    var outsider = user("Outsider", "review-outsider@example.com");
    var course = course("Review Edge", "review-edge", creator, PublishStatus.PUBLISHED);
    enrollment(student, course, EnrollmentStatus.ACTIVE, Instant.now().minusSeconds(1));

    mockMvc
        .perform(
            post("/student/courses/{courseId}/reviews", course.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"rating":5,"reviewText":"Review"}
                    """))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/student/courses/{courseId}/reviews", course.getId())
                .with(authentication(studentAuth(outsider.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"rating":5,"reviewText":"Review"}
                    """))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/student/courses/{courseId}/reviews", course.getId())
                .with(authentication(studentAuth(student.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"rating":5,"reviewText":"Review"}
                    """))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/student/courses/{courseId}/reviews", course.getId())
                .with(authentication(studentAuth(outsider.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"rating":6,"reviewText":"Review"}
                    """))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(get("/admin/reviews").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isForbidden());
    assertThat(courseReviewRepository.count()).isZero();
  }

  private UsernamePasswordAuthenticationToken adminAuthentication() {
    return new UsernamePasswordAuthenticationToken(
        java.util.UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }
}
