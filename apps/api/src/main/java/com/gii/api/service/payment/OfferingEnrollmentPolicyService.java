package com.gii.api.service.payment;

import com.gii.common.entity.course.Course;
import com.gii.common.service.payment.CourseEnrollmentPolicyService;
import com.gii.common.service.payment.EnrollmentPolicyViolationException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OfferingEnrollmentPolicyService {

  private final CourseEnrollmentPolicyService delegate;

  public void validateCheckout(Course course, java.util.UUID userId, Instant now) {
    invoke(() -> delegate.validateCheckout(course, userId, now));
  }

  public void validateActivation(Course course, Instant purchasedAt, Instant now) {
    invoke(() -> delegate.validateActivation(course, purchasedAt, now));
  }

  public Instant calculateExpiry(Course course, Instant enrolledAt) {
    return delegate.calculateExpiry(course, enrolledAt);
  }

  private void invoke(Runnable operation) {
    try {
      operation.run();
    } catch (EnrollmentPolicyViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
    }
  }
}
