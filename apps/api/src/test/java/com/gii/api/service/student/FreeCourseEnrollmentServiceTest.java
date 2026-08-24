package com.gii.api.service.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.payment.OfferingEnrollmentPolicyService;
import com.gii.api.testsupport.CourseTestData;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FreeCourseEnrollmentServiceTest {

  @Mock private CurrentUserService currentUserService;
  @Mock private CourseRepository courseRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private OfferingEnrollmentPolicyService enrollmentPolicyService;
  @Mock private Authentication authentication;

  @InjectMocks private FreeCourseEnrollmentService service;

  @Test
  void createsEnrollmentForPublishedFreeOffering() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    User user = User.builder().fullName("Student").email("student@example.com").build();
    user.setId(userId);
    Course course = CourseTestData.course("Free course", "free-course", user);
    course.setId(courseId);
    course.setStatus(PublishStatus.PUBLISHED);
    course.setIsFree(true);
    Instant expiry = Instant.now().plusSeconds(86_400);
    when(currentUserService.getCurrentUser(authentication)).thenReturn(user);
    when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
    when(enrollmentRepository.findByUserIdAndCourseIdForUpdate(userId, courseId))
        .thenReturn(Optional.empty());
    when(enrollmentPolicyService.calculateExpiry(any(), any())).thenReturn(expiry);

    service.execute(courseId, authentication);

    ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
    verify(enrollmentRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(captor.getValue().getSourceOrderItem()).isNull();
    assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiry);
    verify(enrollmentPolicyService).validateCheckout(any(), any(), any());
  }

  @Test
  void reEnrollmentPreservesCompletedAtOnRefundedEnrollment() {
    assertReEnrollmentPreservesCompletedAt(
        EnrollmentStatus.REFUNDED, Instant.now().plusSeconds(86_400));
  }

  @Test
  void reEnrollmentPreservesCompletedAtOnRevokedEnrollment() {
    assertReEnrollmentPreservesCompletedAt(
        EnrollmentStatus.REVOKED, Instant.now().plusSeconds(86_400));
  }

  @Test
  void reEnrollmentPreservesCompletedAtOnExpiredEnrollment() {
    assertReEnrollmentPreservesCompletedAt(
        EnrollmentStatus.ACTIVE, Instant.now().minusSeconds(60));
  }

  private void assertReEnrollmentPreservesCompletedAt(
      EnrollmentStatus existingStatus, Instant existingExpiresAt) {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    User user = User.builder().fullName("Student").email("student@example.com").build();
    user.setId(userId);
    Course course = CourseTestData.course("Free course", "free-course", user);
    course.setId(courseId);
    course.setStatus(PublishStatus.PUBLISHED);
    course.setIsFree(true);
    Instant completedAt = Instant.now().minusSeconds(500_000);
    Enrollment existing =
        Enrollment.builder()
            .user(user)
            .course(course)
            .status(existingStatus)
            .enrolledAt(Instant.now().minusSeconds(600_000))
            .revokedAt(existingStatus == EnrollmentStatus.REVOKED ? Instant.now() : null)
            .completedAt(completedAt)
            .expiresAt(existingExpiresAt)
            .build();
    Instant newExpiry = Instant.now().plusSeconds(86_400);
    when(currentUserService.getCurrentUser(authentication)).thenReturn(user);
    when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
    when(enrollmentRepository.findByUserIdAndCourseIdForUpdate(userId, courseId))
        .thenReturn(Optional.of(existing));
    when(enrollmentPolicyService.calculateExpiry(any(), any())).thenReturn(newExpiry);

    service.execute(courseId, authentication);

    assertThat(existing.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(existing.getRevokedAt()).isNull();
    assertThat(existing.getExpiresAt()).isEqualTo(newExpiry);
    assertThat(existing.getCompletedAt()).isEqualTo(completedAt);
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  void rejectsPaidOfferingWithoutCreatingEnrollment() {
    UUID courseId = UUID.randomUUID();
    User user = User.builder().fullName("Student").email("student@example.com").build();
    user.setId(UUID.randomUUID());
    Course course = CourseTestData.course("Paid course", "paid-course", user);
    course.setId(courseId);
    course.setIsFree(false);
    when(currentUserService.getCurrentUser(authentication)).thenReturn(user);
    when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> service.execute(courseId, authentication))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409 CONFLICT");

    verify(enrollmentRepository, never()).saveAndFlush(any());
  }
}
