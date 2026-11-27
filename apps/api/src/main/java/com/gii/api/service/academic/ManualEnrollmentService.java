package com.gii.api.service.academic;

import com.gii.api.model.request.admin.ManualEnrollmentRequest;
import com.gii.api.model.response.academic.ManualEnrollmentOverviewResponse;
import com.gii.api.model.response.academic.ManualEnrollmentResponse;
import com.gii.common.entity.academic.ManualEnrollmentAudit;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.ProgramRegistrationStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.academic.ManualEnrollmentAuditRepository;
import com.gii.common.repository.academic.ProgramCourseRegistrationRepository;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.collection.CollectionRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.user.UserRepository;
import com.gii.common.service.payment.CourseEnrollmentPolicyService;
import com.gii.common.service.payment.EnrollmentMutationLockService;
import com.gii.common.service.payment.EnrollmentPolicyViolationException;
import com.gii.common.service.payment.ProgramCourseAccessService;
import com.gii.common.service.payment.ProgramSnapshotService;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ManualEnrollmentService {
  private final UserRepository users;
  private final CourseRepository courses;
  private final CollectionRepository collections;
  private final CollectionCourseRepository collectionCourses;
  private final EnrollmentRepository enrollments;
  private final CollectionEnrollmentRepository programs;
  private final ManualEnrollmentAuditRepository audits;
  private final ProgramCourseRegistrationRepository registrations;
  private final ProgramCourseRegistrationService registrationService;
  private final EnrollmentMutationLockService mutationLock;
  private final CourseEnrollmentPolicyService policy;
  private final ProgramCourseAccessService access;
  private final ProgramSnapshotService snapshots;

  @Transactional(readOnly = true)
  public ManualEnrollmentOverviewResponse overview(UUID userId, int page) {
    var user = users.findById(userId).orElseThrow(() -> missing("Student not found"));
    var programAccess =
        programs.findByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE).stream()
            .map(e ->
                new ManualEnrollmentOverviewResponse.ProgramAccess(
                    e.getCollection().getId(), e.getCollection().getTitle(),
                    ProgramCourseRegistrationService.active(e, Instant.now()), e.getExpiresAt()))
            .toList();
    return new ManualEnrollmentOverviewResponse(
        userId,
        user.getFullName(),
        user.getEmail(),
        user.getPhone(),
        programAccess,
        audits.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, 50)).stream()
            .map(this::response)
            .toList());
  }

  public ManualEnrollmentResponse enroll(UUID userId, UUID actorId, ManualEnrollmentRequest r) {
    validateTarget(r);
    mutationLock.lock(userId);
    var previous = audits.findByRequestId(r.requestId()).orElse(null);
    if (previous != null) {
      if (!sameRequest(previous, userId, actorId, r)) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "This request ID has already been used for a different enrollment action");
      }
      return response(previous);
    }
    users.findById(userId).orElseThrow(() -> missing("Student not found"));
    Grant grant;
    try {
      grant =
          r.programFunded()
              ? register(userId, r)
              : r.courseId() != null ? course(userId, r) : program(userId, r);
    } catch (EnrollmentPolicyViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
    }
    return response(
        audits.saveAndFlush(
            ManualEnrollmentAudit.builder()
                .requestId(r.requestId())
                .userId(userId)
                .actorId(actorId)
                .courseId(r.courseId())
                .programId(r.programId())
                .programFunded(r.programFunded())
                .overrideDates(r.overrideDates())
                .overrideCapacity(r.overrideCapacity())
                .allowUnpublished(r.allowUnpublished())
                .reason(r.reason().trim())
                .enrollmentId(grant.enrollmentId())
                .registrationId(grant.registrationId())
                .accessExpiresAt(grant.expiresAt())
                .changed(grant.changed())
                .build()));
  }

  private Grant course(UUID userId, ManualEnrollmentRequest r) {
    var course =
        courses
            .findByIdForUpdate(r.courseId())
            .orElseThrow(() -> missing("Course offering not found"));
    var e = enrollments.findByUserIdAndCourseIdForUpdate(userId, r.courseId()).orElse(null);
    var now = Instant.now();
    if (e != null
        && e.getStatus() == EnrollmentStatus.ACTIVE
        && (e.getExpiresAt() == null || e.getExpiresAt().isAfter(now))) {
      return new Grant(e.getId(), null, e.getExpiresAt(), false);
    }
    policy.validateManualAdmission(
        course, userId, now, r.overrideDates(), r.overrideCapacity(), r.allowUnpublished());
    if (e == null) {
      e =
          enrollments.saveAndFlush(
              Enrollment.builder()
                  .user(users.getReferenceById(userId))
                  .course(course)
                  .status(EnrollmentStatus.REVOKED)
                  .enrolledAt(now)
                  .build());
    }
    access.prepare(e, now);
    e.setEnrolledAt(now);
    e.setSourceOrderItem(null);
    e.setSourceCollection(null);
    access.independentGrant(e, policy.calculateExpiry(course, now));
    return new Grant(e.getId(), null, e.getExpiresAt(), true);
  }

  private Grant program(UUID userId, ManualEnrollmentRequest r) {
    var collection =
        collections
            .findByIdForUpdate(r.programId())
            .orElseThrow(() -> missing("Program not found"));
    var e = programs.findByUserIdAndCollectionIdForUpdate(userId, r.programId()).orElse(null);
    var now = Instant.now();
    if (e != null && ProgramCourseRegistrationService.active(e, now)) {
      return new Grant(e.getId(), null, e.getExpiresAt(), false);
    }
    if (!r.allowUnpublished() && collection.getStatus() != PublishStatus.PUBLISHED) {
      throw new EnrollmentPolicyViolationException("Program is not published");
    }
    var snapshot = e == null ? null : e.getProgramSnapshot();
    if (snapshot == null) {
      snapshot =
          snapshots.capture(
              collection,
              collectionCourses.findByCollection_IdOrderByPositionAsc(collection.getId()));
      snapshots.validate(snapshot);
    }
    if (e == null) {
      e =
          CollectionEnrollment.builder()
              .user(users.getReferenceById(userId))
              .collection(collection)
              .programSnapshot(snapshot)
              .build();
    } else if (e.getProgramSnapshot() == null) {
      e.setProgramSnapshot(snapshot);
    }
    e.setStatus(EnrollmentStatus.ACTIVE);
    e.setEnrolledAt(now);
    e.setRevokedAt(null);
    e.setSourceOrderItem(null);
    e.setExpiresAt(
        snapshot.creditBased()
            ? now.atZone(ZoneOffset.UTC).plusMonths(snapshot.accessDurationMonths()).toInstant()
            : null);
    programs.saveAndFlush(e);
    // Program access is an entitlement; individual course seats are selected separately.
    return new Grant(e.getId(), null, e.getExpiresAt(), true);
  }

  private Grant register(UUID userId, ManualEnrollmentRequest r) {
    var existing = programs.findByUserIdAndCollectionId(userId, r.programId()).orElse(null);
    boolean selected =
        existing != null
            && registrations
                .findByProgramEnrollmentIdAndStatus(
                    existing.getId(), ProgramRegistrationStatus.ACTIVE)
                .stream()
                .anyMatch(v -> v.getCourseId().equals(r.courseId()));
    var registration =
        registrationService.register(
            userId, r.programId(), r.courseId(), r.overrideDates(), r.overrideCapacity());
    var enrollment = enrollments.findByUserIdAndCourseId(userId, r.courseId()).orElseThrow();
    return new Grant(
        enrollment.getId(), registration.id(), registration.accessExpiresAt(), !selected);
  }

  private void validateTarget(ManualEnrollmentRequest r) {
    if (r.programFunded()
        ? r.courseId() == null || r.programId() == null
        : (r.courseId() == null) == (r.programId() == null)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Select one course or program, or both for a program-funded course registration");
    }
    if (r.courseId() == null && (r.overrideDates() || r.overrideCapacity())
        || r.programFunded() && r.allowUnpublished()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "These overrides do not apply to the selected enrollment type");
    }
  }

  private boolean sameRequest(
      ManualEnrollmentAudit a, UUID userId, UUID actorId, ManualEnrollmentRequest r) {
    return a.getUserId().equals(userId)
        && a.getActorId().equals(actorId)
        && Objects.equals(a.getCourseId(), r.courseId())
        && Objects.equals(a.getProgramId(), r.programId())
        && a.isProgramFunded() == r.programFunded()
        && a.isOverrideDates() == r.overrideDates()
        && a.isOverrideCapacity() == r.overrideCapacity()
        && a.isAllowUnpublished() == r.allowUnpublished()
        && a.getReason().equals(r.reason().trim());
  }

  private ManualEnrollmentResponse response(ManualEnrollmentAudit a) {
    return new ManualEnrollmentResponse(
        a.getId(),
        a.getRequestId(),
        a.getUserId(),
        a.getActorId(),
        users.findById(a.getActorId()).map(u -> u.getFullName()).orElse(null),
        a.getCourseId(),
        a.getCourseId() == null
            ? null
            : courses
                .findById(a.getCourseId())
                .map(c -> c.getName() == null ? c.getTitle() : c.getTitle() + " · " + c.getName())
                .orElse(null),
        a.getProgramId(),
        a.getProgramId() == null
            ? null
            : collections.findById(a.getProgramId()).map(p -> p.getTitle()).orElse(null),
        a.isProgramFunded(),
        a.isOverrideDates(),
        a.isOverrideCapacity(),
        a.isAllowUnpublished(),
        a.getReason(),
        a.getEnrollmentId(),
        a.getRegistrationId(),
        a.getAccessExpiresAt(),
        a.isChanged(),
        a.getCreatedAt());
  }

  private ResponseStatusException missing(String message) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
  }

  private record Grant(
      UUID enrollmentId, UUID registrationId, Instant expiresAt, boolean changed) {}
}
