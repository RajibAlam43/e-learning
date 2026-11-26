package com.gii.api.service.payment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.gii.common.entity.academic.Semester;
import com.gii.common.entity.course.Course;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.service.payment.CourseEnrollmentPolicyService;
import com.gii.common.service.payment.EnrollmentPolicyViolationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SemesterEnrollmentPolicyServiceTest {
  private final CourseEnrollmentPolicyService service =
      new CourseEnrollmentPolicyService(
          mock(EnrollmentRepository.class),
          mock(OrderItemRepository.class),
          mock(com.gii.common.service.payment.CoursePrerequisiteEligibilityService.class));

  @Test
  void semesterRegistrationCannotBeBypassedByMissingOfferingDates() {
    Course course = course();
    assertThatThrownBy(() ->
            service.validateCheckout(
                course, UUID.randomUUID(), Instant.parse("2026-12-01T00:00:00Z")))
        .isInstanceOf(EnrollmentPolicyViolationException.class)
        .hasMessageContaining("not opened");
    assertThatThrownBy(() ->
            service.validateCheckout(
                course, UUID.randomUUID(), Instant.parse("2027-01-15T00:00:00Z")))
        .isInstanceOf(EnrollmentPolicyViolationException.class)
        .hasMessageContaining("closed");
    assertThatCode(() ->
            service.validateCheckout(
                course, UUID.randomUUID(), Instant.parse("2027-01-01T00:00:00Z")))
        .doesNotThrowAnyException();
  }

  @Test
  void paymentActivationUsesTheOriginalPurchaseTimeForSemesterEligibility() {
    assertThatCode(() ->
            service.validateActivation(
                course(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2027-01-01T00:00:00Z"),
                Instant.parse("2027-02-01T00:00:00Z")))
        .doesNotThrowAnyException();
  }

  private Course course() {
    Semester semester =
        Semester.builder()
            .registrationStartsAt(Instant.parse("2026-12-15T00:00:00Z"))
            .registrationEndsAt(Instant.parse("2027-01-15T00:00:00Z"))
            .build();
    return Course.builder().status(PublishStatus.PUBLISHED).semester(semester).build();
  }
}
