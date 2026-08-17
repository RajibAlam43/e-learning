package com.gii.api.service.pub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseReview;
import com.gii.common.entity.user.User;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReviewStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseReviewRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CourseReviewsServiceTest {

  @Mock private CourseRepository courseRepository;
  @Mock private CourseReviewRepository courseReviewRepository;
  @InjectMocks private CourseReviewsService service;

  @Test
  void listsPublishedReviewsWithRatingAndSafePagination() {
    UUID courseId = UUID.randomUUID();
    Course course = Course.builder().title("Course").slug("course").build();
    course.setId(courseId);
    User student = User.builder().fullName("Student").build();
    CourseReview review =
        CourseReview.builder()
            .course(course)
            .user(student)
            .rating(5)
            .reviewText("Excellent")
            .status(ReviewStatus.PUBLISHED)
            .build();
    review.setId(UUID.randomUUID());
    review.setCreatedAt(Instant.now());
    when(courseReviewRepository.findPublicReviews(
            eq(ReviewStatus.PUBLISHED),
            eq(PublishStatus.PUBLISHED),
            eq(5),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenAnswer(
            invocation -> {
              Pageable pageable = invocation.getArgument(3);
              return new PageImpl<>(List.of(review), pageable, 1);
            });

    var response = service.executeAll(5, Pageable.ofSize(100));

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().getFirst().reviewText()).isEqualTo("Excellent");
    assertThat(response.size()).isEqualTo(20);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(courseReviewRepository)
        .findPublicReviews(
            eq(ReviewStatus.PUBLISHED),
            eq(PublishStatus.PUBLISHED),
            eq(5),
            pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending())
        .isTrue();
  }

  @Test
  void rejectsRatingOutsideStarRange() {
    assertThatThrownBy(() -> service.executeAll(0, Pageable.ofSize(20)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("400 BAD_REQUEST");
  }
}
