package com.gii.common.service.payment;

import com.gii.common.entity.course.Course;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseEnrollmentPolicyService {

  private static final Duration PENDING_ORDER_LIFETIME = Duration.ofMinutes(30);

  private final EnrollmentRepository enrollmentRepository;
  private final OrderItemRepository orderItemRepository;

  private final CoursePrerequisiteEligibilityService prerequisites;

  public void validateCheckout(Course course, UUID userId, Instant now) {
    validateCheckout(course, userId, now, null);
  }

  public void validateCheckout(Course course, UUID userId, Instant now, UUID orderId) {
    validateAdmission(course, userId, now, orderId, false, false, false);
  }

  /** Admin exceptions never bypass prerequisite requirements. */
  public void validateManualAdmission(
      Course course,
      UUID userId,
      Instant now,
      boolean overrideDates,
      boolean overrideCapacity,
      boolean allowUnpublished) {
    validateAdmission(course, userId, now, null, overrideDates, overrideCapacity, allowUnpublished);
  }

  private void validateAdmission(
      Course course,
      UUID userId,
      Instant now,
      UUID orderId,
      boolean overrideDates,
      boolean overrideCapacity,
      boolean allowUnpublished) {
    prerequisites.validate(userId, course, orderId);
    if (!allowUnpublished && course.getStatus() != PublishStatus.PUBLISHED) {
      throw new EnrollmentPolicyViolationException("Course is not available");
    }
    if (!overrideDates) {
      validateWindow(course, now);
    }
    if (overrideCapacity || course.getCapacity() == null) {
      return;
    }
    long occupied =
        enrollmentRepository.countAvailableSeatsInUse(course.getId(), EnrollmentStatus.ACTIVE, now);
    long reserved =
        orderItemRepository.countPendingReservations(
            course.getId(), userId, now.minus(PENDING_ORDER_LIFETIME));
    if (occupied + reserved >= course.getCapacity()) {
      throw new EnrollmentPolicyViolationException("Course capacity has been reached");
    }
  }

  public void validateActivation(
      Course course, UUID userId, UUID orderId, Instant purchasedAt, Instant now) {
    prerequisites.validate(userId, course, orderId);
    validatePublishedAndWindow(course, purchasedAt);
    if (course.getCapacity() == null) {
      return;
    }
    long occupied =
        enrollmentRepository.countAvailableSeatsInUse(course.getId(), EnrollmentStatus.ACTIVE, now);
    if (occupied >= course.getCapacity()) {
      throw new EnrollmentPolicyViolationException("Course capacity has been reached");
    }
  }

  public Instant calculateExpiry(Course course, Instant enrolledAt) {
    return course.getAccessDurationDays() == null
        ? null
        : enrolledAt.plus(Duration.ofDays(course.getAccessDurationDays()));
  }

  private void validatePublishedAndWindow(Course course, Instant at) {
    if (course.getStatus() != PublishStatus.PUBLISHED) {
      throw new EnrollmentPolicyViolationException("Course is not available");
    }
    validateWindow(course, at);
  }

  private void validateWindow(Course course, Instant at) {
    if (course.getSemester() != null) {
      if (at.isBefore(course.getSemester().getRegistrationStartsAt())) {
        throw new EnrollmentPolicyViolationException("Semester registration has not opened");
      }
      if (!at.isBefore(course.getSemester().getRegistrationEndsAt())) {
        throw new EnrollmentPolicyViolationException("Semester registration has closed");
      }
    }
    if (course.getEnrollmentStartsAt() != null && at.isBefore(course.getEnrollmentStartsAt())) {
      throw new EnrollmentPolicyViolationException("Course enrollment has not opened");
    }
    if (course.getEnrollmentEndsAt() != null && !at.isBefore(course.getEnrollmentEndsAt())) {
      throw new EnrollmentPolicyViolationException("Course enrollment has closed");
    }
  }
}
