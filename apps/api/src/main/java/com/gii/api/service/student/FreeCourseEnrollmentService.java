package com.gii.api.service.student;

import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.payment.OfferingEnrollmentPolicyService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FreeCourseEnrollmentService {

  private final CurrentUserService currentUserService;
  private final CourseRepository courseRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final OfferingEnrollmentPolicyService enrollmentPolicyService;

  @Transactional
  public void execute(UUID courseId, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    Course course =
        courseRepository
            .findByIdForUpdate(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    if (!Boolean.TRUE.equals(course.getIsFree())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only free courses can be enrolled without payment");
    }

    Instant now = Instant.now();
    Enrollment existing =
        enrollmentRepository.findByUserIdAndCourseIdForUpdate(user.getId(), courseId).orElse(null);
    if (existing != null
        && existing.getStatus() == EnrollmentStatus.ACTIVE
        && (existing.getExpiresAt() == null || existing.getExpiresAt().isAfter(now))) {
      return;
    }

    enrollmentPolicyService.validateCheckout(course, user.getId(), now);
    if (existing == null) {
      enrollmentRepository.saveAndFlush(
          Enrollment.builder()
              .user(user)
              .course(course)
              .status(EnrollmentStatus.ACTIVE)
              .enrolledAt(now)
              .expiresAt(enrollmentPolicyService.calculateExpiry(course, now))
              .build());
      return;
    }

    existing.setStatus(EnrollmentStatus.ACTIVE);
    existing.setEnrolledAt(now);
    existing.setRevokedAt(null);
    existing.setCompletedAt(null);
    existing.setExpiresAt(enrollmentPolicyService.calculateExpiry(course, now));
    existing.setSourceOrderItem(null);
    existing.setSourceCollection(null);
  }
}
