package com.gii.api.service.admin;

import com.gii.api.model.response.admin.AdminCourseReviewResponse;
import com.gii.common.entity.course.CourseReview;
import com.gii.common.enums.ReviewStatus;
import com.gii.common.repository.course.CourseReviewRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCourseReviewManagementService {

  private final CourseReviewRepository courseReviewRepository;

  @Transactional(readOnly = true)
  public List<AdminCourseReviewResponse> list(ReviewStatus status) {
    return courseReviewRepository.findAllForAdmin(status).stream().map(this::toResponse).toList();
  }

  public void publish(UUID reviewId) {
    updateStatus(reviewId, ReviewStatus.PUBLISHED);
  }

  public void unpublish(UUID reviewId) {
    updateStatus(reviewId, ReviewStatus.UNPUBLISHED);
  }

  public void delete(UUID reviewId) {
    courseReviewRepository.delete(find(reviewId));
  }

  private void updateStatus(UUID reviewId, ReviewStatus status) {
    CourseReview review = find(reviewId);
    review.setStatus(status);
    courseReviewRepository.save(review);
  }

  private CourseReview find(UUID reviewId) {
    return courseReviewRepository
        .findById(reviewId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
  }

  private AdminCourseReviewResponse toResponse(CourseReview review) {
    return AdminCourseReviewResponse.builder()
        .reviewId(review.getId())
        .courseId(review.getCourse().getId())
        .courseTitle(review.getCourse().getTitle())
        .studentId(review.getUser().getId())
        .studentName(review.getUser().getFullName())
        .rating(review.getRating())
        .reviewText(review.getReviewText())
        .status(review.getStatus())
        .createdAt(review.getCreatedAt())
        .updatedAt(review.getUpdatedAt())
        .build();
  }
}
