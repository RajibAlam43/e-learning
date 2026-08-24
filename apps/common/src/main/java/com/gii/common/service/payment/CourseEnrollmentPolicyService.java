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

  public void validateCheckout(Course course, UUID userId, Instant now) {
    validatePublishedAndWindow(course, now);
    if (course.getCapacity() == null) {
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

  public void validateActivation(Course course, Instant purchasedAt, Instant now) {
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
    if (course.getEnrollmentStartsAt() != null && at.isBefore(course.getEnrollmentStartsAt())) {
      throw new EnrollmentPolicyViolationException("Course enrollment has not opened");
    }
    if (course.getEnrollmentEndsAt() != null && !at.isBefore(course.getEnrollmentEndsAt())) {
      throw new EnrollmentPolicyViolationException("Course enrollment has closed");
    }
  }
}
