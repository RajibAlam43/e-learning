package com.gii.api.service.progress;

import com.gii.api.service.progress.CourseCompletionService.CourseCompletion;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.repository.course.CourseRepository;
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
  private final CourseRepository courseRepository;
  private final CollectionEnrollmentCompletionService collectionEnrollmentCompletionService;

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
      // This course just flipped to complete; any collection built around it may now be
      // complete too.
      collectionEnrollmentCompletionService.refreshCollectionsForCourse(userId, courseId);
    }
  }

  @Transactional
  public void refreshCourse(UUID courseId) {
    enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE).stream()
        .filter(enrollment -> enrollment.getCompletedAt() == null)
        .forEach(enrollment -> refresh(enrollment.getUser().getId(), courseId));
  }

  /**
   * Re-checks completion for every active, not-yet-completed enrollment across every offering
   * built on the given template. Call this after a curriculum mutation that can shrink the
   * completion denominator (deleting/unpublishing a lesson, quiz, section, or live-class slot;
   * demoting a mandatory item to optional) so learners who now satisfy the reduced requirements
   * are marked complete immediately rather than on their next unrelated progress event.
   */
  @Transactional
  public void refreshCoursesForTemplate(UUID templateId) {
    courseRepository.findByTemplateId(templateId).forEach(course -> refreshCourse(course.getId()));
  }
}
