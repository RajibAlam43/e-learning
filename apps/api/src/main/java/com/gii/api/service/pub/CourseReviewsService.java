package com.gii.api.service.pub;

import com.gii.api.model.response.CourseReviewResponse;
import com.gii.api.model.response.PageResponse;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseReview;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReviewStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseReviewRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewsService {

  private static final int MAX_PAGE_SIZE = 20;

  private final CourseRepository courseRepository;
  private final CourseReviewRepository courseReviewRepository;

  public List<CourseReviewResponse> execute(String courseSlug) {
    Course course =
        courseRepository
            .findBySlugAndStatus(courseSlug, PublishStatus.PUBLISHED)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    return courseReviewRepository
        .findByCourseIdAndStatus(course.getId(), ReviewStatus.PUBLISHED)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public PageResponse<CourseReviewResponse> executeAll(Integer rating, Pageable pageable) {
    if (rating != null && (rating < 1 || rating > 5)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
    }

    Pageable safePageable =
        PageRequest.of(
            Math.max(pageable.getPageNumber(), 0),
            Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE),
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    Page<CourseReview> reviews =
        courseReviewRepository.findPublicReviews(
            ReviewStatus.PUBLISHED, PublishStatus.PUBLISHED, rating, safePageable);

    return PageResponse.<CourseReviewResponse>builder()
        .content(reviews.getContent().stream().map(this::toResponse).toList())
        .page(reviews.getNumber())
        .size(reviews.getSize())
        .totalElements(reviews.getTotalElements())
        .totalPages(reviews.getTotalPages())
        .build();
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
