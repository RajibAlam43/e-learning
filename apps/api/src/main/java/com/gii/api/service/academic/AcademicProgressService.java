package com.gii.api.service.academic;

import com.gii.api.model.response.academic.AcademicProgressResponse;
import com.gii.api.model.response.academic.ProgramGraduationResponse;
import com.gii.api.model.response.academic.ProgramGraduationResponse.Status;
import com.gii.api.service.progress.CollectionEnrollmentCompletionService;
import com.gii.api.service.progress.CollectionRequirementsCompletionService;
import com.gii.api.service.progress.ProgramCreditProgressService;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.repository.academic.SemesterRepository;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicProgressService {
  private final UserRepository users;
  private final AcademicAchievementService achievements;
  private final SemesterRepository semesters;
  private final CollectionEnrollmentRepository enrollments;
  private final ProgramCreditProgressService credits;
  private final CollectionRequirementsCompletionService requirements;
  private final CollectionEnrollmentCompletionService completion;
  private final CertificateRepository certificates;

  public AcademicProgressResponse get(UUID userId) {
    var user =
        users
            .findById(userId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    var record = achievements.get(userId);
    var semesterIds =
        record.achievements().stream()
            .map(a -> a.semesterId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    var labels =
        semesters.findAllById(semesterIds).stream()
            .map(s ->
                new AcademicProgressResponse.SemesterLabel(s.getId(), s.getCode(), s.getName()))
            .toList();
    return new AcademicProgressResponse(
        userId,
        user.getFullName(),
        user.getEmail(),
        user.getPhone(),
        record,
        labels,
        enrollments.findByUserIdOrderByEnrolledAtDesc(userId).stream().map(this::report).toList());
  }

  public ProgramGraduationResponse getProgram(UUID userId, UUID programId) {
    return report(enrollment(userId, programId));
  }

  @Transactional
  public ProgramGraduationResponse check(UUID userId, UUID programId) {
    var e =
        enrollments
            .findByUserIdAndCollectionIdForUpdate(userId, programId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Program enrollment not found"));
    if (e.getCompletedAt() == null) {
      if (!e.isAccessActiveAt(Instant.now())) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Program access is inactive or expired; contact an administrator");
      }
      completion.refresh(userId, programId);
    }
    return report(e);
  }

  private CollectionEnrollment enrollment(UUID userId, UUID programId) {
    return enrollments
        .findByUserIdAndCollectionId(userId, programId)
        .orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Program enrollment not found"));
  }

  private ProgramGraduationResponse report(CollectionEnrollment e) {
    var breakdown = credits.breakdown(e);
    var progress = breakdown == null ? null : breakdown.progress();
    boolean met = progress == null ? requirements.isComplete(e) : progress.complete();
    boolean active = e.isAccessActiveAt(Instant.now());
    var certificate =
        certificates
            .findByUserIdAndCollectionId(e.getUser().getId(), e.getCollection().getId())
            .orElse(null);
    boolean revoked = certificate != null && certificate.getRevokedAt() != null;
    var status =
        e.getCompletedAt() != null
            ? Status.COMPLETED
            : !active ? Status.ACCESS_INACTIVE : met ? Status.ELIGIBLE : Status.IN_PROGRESS;
    return new ProgramGraduationResponse(
        e.getCollection().getId(),
        e.getCollection().getTitle(),
        e.getProgramSnapshot() == null
            ? e.getCollection().getType()
            : e.getProgramSnapshot().collectionType(),
        e.getStatus(),
        active,
        e.getExpiresAt(),
        e.getCompletedAt(),
        status,
        met,
        active && e.getCompletedAt() == null,
        !revoked && (certificate != null || active && (met || e.getCompletedAt() != null)),
        e.getProgramSnapshot(),
        progress,
        breakdown == null ? List.of() : breakdown.courses(),
        progress == null ? 0 : Math.max(0, progress.requiredCredits() - progress.earnedCredits()),
        certificate == null ? null : certificate.getId(),
        certificate == null ? null : certificate.getCertificateCode(),
        revoked);
  }
}
