package com.gii.api.service.pub;

import com.gii.api.model.response.CourseReviewResponse;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseReview;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReviewStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseReviewRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewsService {

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
