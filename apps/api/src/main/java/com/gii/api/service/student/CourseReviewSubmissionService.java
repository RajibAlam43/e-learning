package com.gii.api.service.student;

import com.gii.api.model.request.student.CreateCourseReviewRequest;
import com.gii.api.model.response.CourseReviewResponse;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseReviewSubmissionService {

  private final CurrentUserService currentUserService;
  private final CourseRepository courseRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CourseReviewRepository courseReviewRepository;

  public CourseReviewResponse execute(
      UUID courseId, CreateCourseReviewRequest request, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    Course course =
        courseRepository
            .findById(courseId)
            .filter(found -> found.getStatus() == PublishStatus.PUBLISHED)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    Enrollment enrollment =
        enrollmentRepository
            .findByUserIdAndCourseId(user.getId(), courseId)
            .filter(found -> found.getStatus() == EnrollmentStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Active course enrollment is required"));
    if (enrollment.getExpiresAt() != null
        && enrollment.getExpiresAt().isBefore(java.time.Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Course enrollment has expired");
    }
    if (courseReviewRepository.existsByCourseIdAndUserId(courseId, user.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A review for this course already exists");
    }

    CourseReview review =
        CourseReview.builder()
            .course(course)
            .user(user)
            .rating(request.rating())
            .reviewText(request.reviewText().trim())
            .status(ReviewStatus.PENDING)
            .build();
    try {
      return toResponse(courseReviewRepository.saveAndFlush(review));
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A review for this course already exists", exception);
    }
  }

  private CourseReviewResponse toResponse(CourseReview review) {
    return CourseReviewResponse.builder()
        .reviewId(review.getId())
        .courseId(review.getCourse().getId())
        .studentName(review.getUser().getFullName())
        .rating(review.getRating())
        .reviewText(review.getReviewText())
        .createdAt(review.getCreatedAt())
        .build();
  }
}
