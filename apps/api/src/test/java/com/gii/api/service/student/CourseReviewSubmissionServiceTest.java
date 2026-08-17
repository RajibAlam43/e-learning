package com.gii.api.service.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gii.api.model.request.student.CreateCourseReviewRequest;
import com.gii.api.model.request.student.UpdateCourseReviewRequest;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseReview;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReviewStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseReviewRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CourseReviewSubmissionServiceTest {

  @Mock private CurrentUserService currentUserService;
  @Mock private CourseRepository courseRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private CourseReviewRepository courseReviewRepository;
  @Mock private Authentication authentication;

  @InjectMocks private CourseReviewSubmissionService service;

  @Test
  void enrolledStudentCreatesPendingReview() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    User user = User.builder().fullName("Student").email("student@example.com").build();
    user.setId(userId);
    Course course =
        Course.builder().title("Course").slug("course").status(PublishStatus.PUBLISHED).build();
    course.setId(courseId);
    Enrollment enrollment =
        Enrollment.builder()
            .user(user)
            .course(course)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now())
            .build();
    when(currentUserService.getCurrentUser(authentication)).thenReturn(user);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId))
        .thenReturn(Optional.of(enrollment));
    when(courseReviewRepository.existsByCourseIdAndUserId(courseId, userId)).thenReturn(false);
    when(courseReviewRepository.saveAndFlush(any(CourseReview.class)))
        .thenAnswer(
            invocation -> {
              CourseReview review = invocation.getArgument(0);
              review.setId(UUID.randomUUID());
              review.setCreatedAt(Instant.now());
              return review;
            });

    var response =
        service.execute(
            courseId, new CreateCourseReviewRequest(5, "  Excellent course  "), authentication);

    assertThat(response.rating()).isEqualTo(5);
    assertThat(response.reviewText()).isEqualTo("Excellent course");
    ArgumentCaptor<CourseReview> reviewCaptor = ArgumentCaptor.forClass(CourseReview.class);
    verify(courseReviewRepository).saveAndFlush(reviewCaptor.capture());
    assertThat(reviewCaptor.getValue().getStatus()).isEqualTo(ReviewStatus.PENDING);
  }

  @Test
  void duplicateReviewIsRejected() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    User user = User.builder().fullName("Student").email("student@example.com").build();
    user.setId(userId);
    Course course =
        Course.builder().title("Course").slug("course").status(PublishStatus.PUBLISHED).build();
    course.setId(courseId);
    Enrollment enrollment =
        Enrollment.builder().user(user).course(course).status(EnrollmentStatus.ACTIVE).build();
    when(currentUserService.getCurrentUser(authentication)).thenReturn(user);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId))
        .thenReturn(Optional.of(enrollment));
    when(courseReviewRepository.existsByCourseIdAndUserId(courseId, userId)).thenReturn(true);

    assertThatThrownBy(
            () ->
                service.execute(
                    courseId, new CreateCourseReviewRequest(4, "Helpful"), authentication))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409 CONFLICT");
  }

  @Test
  void studentUpdatesAndDeletesOwnedReview() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    User user = User.builder().fullName("Student").email("student@example.com").build();
    user.setId(userId);
    Course course = Course.builder().title("Course").slug("course").build();
    course.setId(courseId);
    CourseReview review =
        CourseReview.builder()
            .course(course)
            .user(user)
            .rating(5)
            .reviewText("Original")
            .status(ReviewStatus.PUBLISHED)
            .build();
    review.setId(UUID.randomUUID());
    review.setCreatedAt(Instant.now());
    when(currentUserService.getCurrentUserId(authentication)).thenReturn(userId);
    when(courseReviewRepository.findByCourseIdAndUserId(courseId, userId))
        .thenReturn(Optional.of(review));
    when(courseReviewRepository.save(review)).thenReturn(review);

    var response =
        service.update(
            courseId, new UpdateCourseReviewRequest(4, "  Updated review  "), authentication);

    assertThat(response.rating()).isEqualTo(4);
    assertThat(response.reviewText()).isEqualTo("Updated review");
    assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING);

    service.delete(courseId, authentication);

    verify(courseReviewRepository).delete(review);
  }
}
