package com.gii.api.service.academic;

import com.gii.api.model.response.academic.ProgramRegistrationResponse;
import com.gii.api.service.collection.PurchasedCollectionCoursesService;
import com.gii.api.service.progress.CourseCompletionService;
import com.gii.common.entity.academic.ProgramCourseRegistration;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.ProgramRegistrationStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.academic.ProgramCourseRegistrationRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.user.UserRepository;
import com.gii.common.service.payment.CourseEnrollmentPolicyService;
import com.gii.common.service.payment.CoursePrerequisiteEligibilityService;
import com.gii.common.service.payment.EnrollmentMutationLockService;
import com.gii.common.service.payment.EnrollmentPolicyViolationException;
import com.gii.common.service.payment.ProgramCourseAccessService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProgramCourseRegistrationService {
  private final CollectionEnrollmentRepository programs;
  private final CourseRepository courses;
  private final EnrollmentRepository enrollments;
  private final ProgramCourseRegistrationRepository registrations;
  private final PurchasedCollectionCoursesService curriculum;
  private final CourseEnrollmentPolicyService policy;
  private final CoursePrerequisiteEligibilityService prerequisites;
  private final ProgramRegistrationAllowanceService allowance;
  private final EnrollmentMutationLockService mutationLock;
  private final ProgramCourseAccessService access;
  private final CourseCompletionService completion;
  private final UserRepository users;

  @Transactional
  public ProgramRegistrationResponse register(UUID userId, UUID programId, UUID courseId) {
    return register(userId, programId, courseId, false, false);
  }

  @Transactional
  public ProgramRegistrationResponse register(
      UUID userId, UUID programId, UUID courseId, boolean overrideDates, boolean overrideCapacity) {
    mutationLock.lock(userId);
    var course =
        courses
            .findByIdForUpdate(courseId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Course offering not found"));
    final var lockedEnrollment =
        enrollments.findByUserIdAndCourseIdForUpdate(userId, courseId).orElse(null);
    var program =
        programs
            .findByUserIdAndCollectionIdForUpdate(userId, programId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Program enrollment not found"));
    Instant now = Instant.now();
    if (!active(program, now)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Program access is inactive or has expired");
    }
    var selections =
        registrations.findByProgramEnrollmentIdAndStatus(
            program.getId(), ProgramRegistrationStatus.ACTIVE);
    UUID template = course.getTemplate().getId();
    var selected =
        selections.stream()
            .filter(r -> r.getCourseTemplateId().equals(template))
            .findFirst()
            .orElse(null);
    if (selected != null) {
      if (!selected.getCourseId().equals(courseId)) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Another cohort is already selected for this course; "
                + "ask an administrator to change it");
      }
      // Retrying an existing selection never renews access or consumes another seat/credit.
      return response(selected);
    }
    final var allowed =
        curriculum.resolveItems(program).stream()
            .filter(c -> c.course().getTemplate().getId().equals(template))
            .findFirst()
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This course curriculum is not included in your program"));
    if (course.getStatus() != PublishStatus.PUBLISHED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This course offering is not published yet");
    }
    if (prerequisites.hasCredit(userId, template)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "You already have academic credit for this course; registration is not required");
    }
    var existing = lockedEnrollment;
    if (existing != null && existing.getCompletedAt() != null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "This course is already completed; request a credit waiver if its credits are missing");
    }
    boolean occupied =
        existing != null
            && existing.getStatus() == EnrollmentStatus.ACTIVE
            && (existing.getExpiresAt() == null || existing.getExpiresAt().isAfter(now));
    boolean another =
        enrollments
            .findByUserIdAndTemplateIdAndStatus(userId, template, EnrollmentStatus.ACTIVE)
            .stream()
            .anyMatch(e ->
                !e.getCourse().getId().equals(courseId)
                    && e.getCompletedAt() == null
                    && (e.getExpiresAt() == null || e.getExpiresAt().isAfter(now)));
    if (another) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "You are already enrolled in another cohort of this course");
    }
    var budget = allowance.get(program, selections);
    if (!allowed.mandatory()
        && budget.remaining() != null
        && course.getCredits() > budget.remaining()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This course exceeds your remaining elective credit allowance");
    }
    if (!occupied) {
      try {
        policy.validateManualAdmission(course, userId, now, overrideDates, overrideCapacity, false);
      } catch (EnrollmentPolicyViolationException exception) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
      }
    }
    if (existing == null) {
      existing =
          enrollments.saveAndFlush(
              Enrollment.builder()
                  .user(program.getUser())
                  .course(course)
                  .status(EnrollmentStatus.REVOKED)
                  .enrolledAt(now)
                  .sourceCollection(program.getCollection())
                  .sourceOrderItem(program.getSourceOrderItem())
                  .build());
    }
    access.prepare(existing, now);
    if (!occupied) {
      existing.setEnrolledAt(now);
    }
    var expiry =
        ProgramCourseAccessService.earlier(
            program.getExpiresAt(), policy.calculateExpiry(course, now));
    var registration =
        registrations.saveAndFlush(
            ProgramCourseRegistration.builder()
                .programEnrollment(program)
                .courseTemplateId(template)
                .courseId(courseId)
                .enrollment(existing)
                .credits(course.getCredits())
                .mandatory(allowed.mandatory())
                .status(ProgramRegistrationStatus.ACTIVE)
                .accessExpiresAt(expiry)
                .build());
    access.refresh(existing, now);
    return response(registration);
  }

  @Transactional
  public ProgramRegistrationResponse withdraw(UUID userId, UUID id, UUID reviewer, String reason) {
    mutationLock.lock(userId);
    var r =
        registrations
            .findById(id)
            .filter(v -> v.getProgramEnrollment().getUser().getId().equals(userId))
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Program registration not found"));
    courses.findByIdForUpdate(r.getCourseId()).orElseThrow();
    enrollments.findByUserIdAndCourseIdForUpdate(userId, r.getCourseId()).orElseThrow();
    programs
        .findByUserIdAndCollectionIdForUpdate(
            userId, r.getProgramEnrollment().getCollection().getId())
        .orElseThrow();
    if (r.getStatus() == ProgramRegistrationStatus.WITHDRAWN) {
      return response(r);
    }
    if (!canWithdraw(r)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Completed courses and awarded credits cannot be withdrawn");
    }
    r.setStatus(ProgramRegistrationStatus.WITHDRAWN);
    r.setWithdrawnBy(reviewer);
    r.setWithdrawnAt(Instant.now());
    r.setWithdrawalReason(reason.trim());
    registrations.saveAndFlush(r);
    access.refresh(r.getEnrollment(), Instant.now());
    return response(r);
  }

  @Transactional(readOnly = true)
  public List<ProgramRegistrationResponse> history(UUID userId) {
    return history(userId, 0);
  }

  @Transactional(readOnly = true)
  public List<ProgramRegistrationResponse> history(UUID userId, int page) {
    return registrations
        .findByProgramEnrollmentUserIdOrderByCreatedAtDesc(
            userId, org.springframework.data.domain.PageRequest.of(page, 50))
        .stream()
        .map(this::response)
        .toList();
  }

  public boolean canWithdraw(ProgramCourseRegistration r) {
    if (r.getStatus() != ProgramRegistrationStatus.ACTIVE
        || r.getEnrollment().getCompletedAt() != null
        || prerequisites.hasCredit(
            r.getProgramEnrollment().getUser().getId(), r.getCourseTemplateId())) {
      return false;
    }
    var p = completion.get(r.getProgramEnrollment().getUser().getId(), r.getCourseId());
    return !p.permanentlyCompleted()
        && !(p.totalItems() > 0 && p.completedItems() >= p.totalItems());
  }

  public ProgramRegistrationResponse response(ProgramCourseRegistration r) {
    var p = r.getProgramEnrollment();
    var c = r.getEnrollment().getCourse();
    return new ProgramRegistrationResponse(
        r.getId(),
        p.getCollection().getId(),
        p.getCollection().getTitle(),
        r.getCourseTemplateId(),
        r.getCourseId(),
        c.getTitle(),
        c.getName() == null ? c.getSlug() : c.getName(),
        r.getCredits(),
        r.isMandatory(),
        r.getStatus(),
        r.getCreatedAt(),
        r.getAccessExpiresAt(),
        r.getWithdrawnAt(),
        r.getWithdrawnBy() == null
            ? null
            : users.findById(r.getWithdrawnBy()).map(u -> u.getFullName()).orElse(null),
        r.getWithdrawalReason(),
        canWithdraw(r));
  }

  public static boolean active(CollectionEnrollment e, Instant now) {
    return e.isAccessActiveAt(now);
  }
}
