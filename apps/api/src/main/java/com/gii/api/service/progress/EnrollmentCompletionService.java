package com.gii.api.service.progress;

import com.gii.api.service.progress.CourseCompletionService.CourseCompletion;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentCompletionService {

  private final EnrollmentRepository enrollmentRepository;
  private final CourseCompletionService courseCompletionService;

  @Transactional
  public void refresh(UUID userId, UUID courseId) {
    Enrollment enrollment =
        enrollmentRepository.findByUserIdAndCourseIdForUpdate(userId, courseId).orElse(null);
    if (enrollment == null
        || enrollment.getStatus() != EnrollmentStatus.ACTIVE
        || enrollment.getCompletedAt() != null) {
      return;
    }
    CourseCompletion completion = courseCompletionService.get(userId, courseId);
    if (completion.totalItems() > 0 && completion.completedItems() >= completion.totalItems()) {
      enrollment.setCompletedAt(Instant.now());
      enrollmentRepository.save(enrollment);
    }
  }

  @Transactional
  public void refreshCourse(UUID courseId) {
    enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE).stream()
        .filter(enrollment -> enrollment.getCompletedAt() == null)
        .forEach(enrollment -> refresh(enrollment.getUser().getId(), courseId));
  }
}
