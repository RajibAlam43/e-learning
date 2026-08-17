package com.gii.api.service.lesson;

import com.gii.api.model.response.lesson.CourseProgressResponse;
import com.gii.api.service.progress.CourseCompletionService;
import com.gii.api.service.progress.CourseCompletionService.CourseCompletion;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseProgressService {

  private final LessonAccessService lessonAccessService;
  private final EnrollmentRepository enrollmentRepository;
  private final CourseCompletionService courseCompletionService;

  public CourseProgressResponse execute(UUID courseId, Authentication authentication) {
    UUID userId = lessonAccessService.requireCurrentUserId(authentication);
    enrollmentRepository
        .findByUserIdAndCourseId(userId, courseId)
        .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Course not found or not enrolled"));

    CourseCompletion courseCompletion = courseCompletionService.get(userId, courseId);
    return CourseProgressResponse.builder()
        .courseId(courseId)
        .completionPercentage(courseCompletion.completionPercentage())
        .build();
  }
}
