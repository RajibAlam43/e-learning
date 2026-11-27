package com.gii.api.service.academic;

import com.gii.api.model.request.admin.CreateWaiverRequest;
import com.gii.api.model.request.admin.ReviewWaiverRequest;
import com.gii.api.model.response.academic.WaiverResponse;
import com.gii.api.service.progress.CollectionEnrollmentCompletionService;
import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.entity.academic.CourseWaiverRequest;
import com.gii.common.entity.course.CourseTemplate;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.WaiverStatus;
import com.gii.common.repository.academic.AcademicAchievementRepository;
import com.gii.common.repository.academic.CourseWaiverRequestRepository;
import com.gii.common.repository.academic.SemesterRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.course.CourseTemplateRepository;
import com.gii.common.repository.user.UserRepository;
import com.gii.common.service.payment.CoursePrerequisiteEligibilityService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseWaiverService {
  private final CourseWaiverRequestRepository requests;
  private final CourseTemplateRepository templates;
  private final AcademicAchievementRepository achievements;
  private final SemesterRepository semesters;
  private final UserRepository users;
  private final JdbcTemplate jdbc;
  private final CoursePrerequisiteEligibilityService eligibility;
  private final CoursePrerequisiteManagementService prerequisites;
  private final WaiverEvidenceService evidence;
  private final CollectionEnrollmentRepository collections;
  private final CollectionEnrollmentCompletionService completion;

  public List<WaiverResponse> mine(UUID user, int page) {
    return requests.findByUserIdOrderByCreatedAtDesc(user, PageRequest.of(page, 50)).stream()
        .map(this::response)
        .toList();
  }

  public List<WaiverResponse> list(WaiverStatus status, int page) {
    return requests.findByStatusOrderByCreatedAtAsc(status, PageRequest.of(page, 50)).stream()
        .map(this::response)
        .toList();
  }

  @Transactional
  public WaiverResponse create(UUID user, CreateWaiverRequest request) {
    jdbc.queryForList("SELECT id FROM users WHERE id=? FOR NO KEY UPDATE", user);
    if (prerequisites.waiverCourses().stream()
        .noneMatch(c -> c.courseTemplateId().equals(request.courseTemplateId()))) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Course is not available for a waiver request");
    }
    if (eligibility.hasCredit(user, request.courseTemplateId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Credits have already been awarded for this course");
    }
    if (requests.existsByUserIdAndCourseTemplateIdAndStatus(
        user, request.courseTemplateId(), WaiverStatus.PENDING)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A waiver request for this course is already pending");
    }
    evidence.validate(user, request.evidenceObjectKey());
    CourseTemplate t = templates.findById(request.courseTemplateId()).orElseThrow();
    return response(
        requests.save(
            CourseWaiverRequest.builder()
                .userId(user)
                .courseTemplateId(t.getId())
                .courseTitle(title(t))
                .requestedCredits(t.getCredits())
                .reason(request.reason().trim())
                .evidenceObjectKey(request.evidenceObjectKey())
                .status(WaiverStatus.PENDING)
                .build()));
  }

  @Transactional
  public WaiverResponse review(UUID id, UUID reviewer, ReviewWaiverRequest review) {
    if (review.decision() == WaiverStatus.PENDING) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose APPROVED or REJECTED");
    }
    var w =
        requests
            .findForUpdate(id)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiver request not found"));
    if (w.getStatus() != WaiverStatus.PENDING) {
      if (w.getStatus() == review.decision()) {
        return response(w);
      }
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This request has already been reviewed");
    }
    jdbc.queryForList(
        "SELECT id FROM course_templates WHERE id=? FOR NO KEY UPDATE", w.getCourseTemplateId());
    var t = templates.findById(w.getCourseTemplateId()).orElseThrow();
    // Refresh to observe credit edits committed while the reviewer had the screen open.
    int credits =
        jdbc.queryForObject(
            "SELECT credits FROM course_templates WHERE id=?", Integer.class, t.getId());
    if (review.decision() == WaiverStatus.APPROVED) {
      if (credits != review.expectedCredits()) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Course credits changed; refresh before approving");
      }
      if (review.semesterId() != null && !semesters.existsById(review.semesterId())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Semester not found");
      }
      requests.flush();
      jdbc.update(
          """
          INSERT INTO academic_achievements(user_id,course_template_id,semester_id,course_title,credits,source,achieved_at,waiver_request_id,awarded_by,award_reason)
          VALUES(?,?,?,?,?,'EXEMPTION',now(),?,?,?) ON CONFLICT(user_id,course_template_id) DO NOTHING
          """,
          w.getUserId(),
          t.getId(),
          review.semesterId(),
          title(t),
          credits,
          w.getId(),
          reviewer,
          review.reason().trim());
      w.setAchievementId(
          jdbc.queryForObject(
              "SELECT id FROM academic_achievements WHERE user_id=? AND course_template_id=?",
              UUID.class,
              w.getUserId(),
              t.getId()));
    }
    w.setStatus(review.decision());
    w.setReviewerId(reviewer);
    w.setReviewReason(review.reason().trim());
    w.setReviewedAt(Instant.now());
    requests.saveAndFlush(w);
    if (w.getStatus() == WaiverStatus.APPROVED) {
      for (var c : collections.findByUserIdAndStatus(w.getUserId(), EnrollmentStatus.ACTIVE)) {
        completion.refresh(w.getUserId(), c.getCollection().getId());
      }
    }
    return response(w);
  }

  public R2PresignedUrlService.PresignedDownload download(UUID id, UUID user, boolean admin) {
    var w =
        requests
            .findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiver request not found"));
    if (!admin && !w.getUserId().equals(user)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiver request not found");
    }
    if (w.getEvidenceObjectKey() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No supporting file attached");
    }
    return evidence.download(w.getEvidenceObjectKey());
  }

  private WaiverResponse response(CourseWaiverRequest w) {
    var user = users.findById(w.getUserId()).orElseThrow();
    var t = templates.findById(w.getCourseTemplateId()).orElseThrow();
    var award =
        w.getAchievementId() == null
            ? null
            : achievements.findById(w.getAchievementId()).orElse(null);
    String reviewer =
        w.getReviewerId() == null
            ? null
            : users.findById(w.getReviewerId()).map(u -> u.getFullName()).orElse(null);
    return new WaiverResponse(
        w.getId(),
        user.getId(),
        user.getFullName(),
        user.getEmail() != null ? user.getEmail() : user.getPhone(),
        t.getId(),
        w.getCourseTitle(),
        w.getRequestedCredits(),
        t.getCredits(),
        w.getReason(),
        w.getEvidenceObjectKey() != null,
        w.getStatus(),
        w.getReviewReason(),
        reviewer,
        w.getReviewedAt(),
        w.getCreatedAt(),
        w.getAchievementId(),
        award == null ? null : award.getCredits());
  }

  private String title(CourseTemplate t) {
    return t.getTitleEn() != null && !t.getTitleEn().isBlank() ? t.getTitleEn() : t.getTitle();
  }
}
