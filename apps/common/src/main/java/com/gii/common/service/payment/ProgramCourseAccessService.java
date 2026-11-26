package com.gii.common.service.payment;

import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.ProgramRegistrationStatus;
import com.gii.common.repository.academic.ProgramCourseRegistrationRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Combines independent access and program grants without shortening another valid source. */
@Service
@RequiredArgsConstructor
@Transactional
public class ProgramCourseAccessService {
  private final EnrollmentRepository enrollments;
  private final ProgramCourseRegistrationRepository registrations;

  public void prepare(Enrollment e, Instant now) {
    if (e.isProgramAccessManaged()) {
      return;
    }
    boolean independent =
        e.getStatus() == EnrollmentStatus.ACTIVE
            && (e.getExpiresAt() == null || e.getExpiresAt().isAfter(now));
    e.setProgramAccessManaged(true);
    e.setIndependentAccessAvailable(independent);
    e.setIndependentAccessExpiresAt(independent ? e.getExpiresAt() : null);
  }

  public void independentGrant(Enrollment e, Instant expiry) {
    if (!e.isProgramAccessManaged()) {
      e.setExpiresAt(expiry);
      return;
    }
    if (!e.isIndependentAccessAvailable()
        || e.getIndependentAccessExpiresAt() != null
            && (expiry == null || expiry.isAfter(e.getIndependentAccessExpiresAt()))) {
      e.setIndependentAccessExpiresAt(expiry);
    }
    e.setIndependentAccessAvailable(true);
    refresh(e, Instant.now());
  }

  public void lockManagedEnrollments(UUID userId) {
    registrations.findByUser(userId).stream()
        .map(r -> r.getEnrollment().getCourse().getId())
        .distinct()
        .sorted()
        .forEach(id -> enrollments.findByUserIdAndCourseIdForUpdate(userId, id));
  }

  public void refreshUser(UUID userId) {
    registrations.findByUser(userId).stream()
        .map(r -> r.getEnrollment())
        .distinct()
        .forEach(e -> refresh(e, Instant.now()));
  }

  public void refresh(Enrollment e, Instant now) {
    if (!e.isProgramAccessManaged()) {
      return;
    }
    boolean valid =
        e.isIndependentAccessAvailable()
            && (e.getIndependentAccessExpiresAt() == null
                || e.getIndependentAccessExpiresAt().isAfter(now));
    Instant latest = valid ? e.getIndependentAccessExpiresAt() : null;
    for (var r :
        registrations.findByEnrollmentIdAndStatus(e.getId(), ProgramRegistrationStatus.ACTIVE)) {
      var p = r.getProgramEnrollment();
      if (p.getStatus() != EnrollmentStatus.ACTIVE
          || p.getExpiresAt() != null && !p.getExpiresAt().isAfter(now)
          || p.getSourceOrderItem() != null
              && p.getSourceOrderItem().getOrder().getStatus()
                  != com.gii.common.enums.OrderStatus.PAID) {
        continue;
      }
      Instant end = earlier(r.getAccessExpiresAt(), p.getExpiresAt());
      if (end != null && !end.isAfter(now)) {
        continue;
      }
      if (!valid) {
        valid = true;
        latest = end;
      } else if (latest != null && (end == null || end.isAfter(latest))) {
        latest = end;
      }
    }
    if (valid) {
      e.setStatus(EnrollmentStatus.ACTIVE);
      e.setRevokedAt(null);
      e.setExpiresAt(latest);
    } else {
      if (e.getStatus() != EnrollmentStatus.REFUNDED) {
        e.setStatus(EnrollmentStatus.REVOKED);
      }
      e.setRevokedAt(now);
      e.setExpiresAt(now);
    }
    enrollments.save(e);
  }

  public static Instant earlier(Instant a, Instant b) {
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return a.isBefore(b) ? a : b;
  }
}
