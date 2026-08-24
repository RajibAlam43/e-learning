package com.gii.api.service.quiz;

import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.enrollment.CurriculumAccessService;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.quiz.QuizAttempt;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.quiz.QuizRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizAccessService {

  private final CurrentUserService currentUserService;
  private final QuizRepository quizRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CurriculumAccessService curriculumAccessService;

  public UUID requireCurrentUserId(Authentication authentication) {
    return currentUserService.getCurrentUserId(authentication);
  }

  public User requireCurrentUser(Authentication authentication) {
    return currentUserService.getCurrentUser(authentication);
  }

  public Quiz requirePublishedQuiz(UUID quizId) {
    Quiz quiz =
        quizRepository
            .findByIdAndStatus(quizId, PublishStatus.PUBLISHED)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
    if (quiz.getSection().getStatus() != PublishStatus.PUBLISHED) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
    }
    return quiz;
  }

  public Enrollment requireActiveEnrollment(UUID userId, UUID courseId, Quiz quiz) {
    Enrollment enrollment =
        enrollmentRepository
            .findByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You do not have access to this quiz"));
    if (!enrollment
        .getCourse()
        .getTemplateVersion()
        .getId()
        .equals(quiz.getSection().getTemplateVersion().getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found in course");
    }
    curriculumAccessService.requireSectionAccess(
        quiz.getSection(), enrollment, Instant.now(), "Quiz");
    return enrollment;
  }

  public Enrollment requireActiveEnrollment(UUID userId, Quiz quiz) {
    var enrollments =
        enrollmentRepository
            .findByUserIdAndTemplateVersionIdAndStatus(
                userId, quiz.getSection().getTemplateVersion().getId(), EnrollmentStatus.ACTIVE)
            .stream()
            .filter(e -> !curriculumAccessService.isEnrollmentExpired(e, Instant.now()))
            .toList();
    if (enrollments.size() > 1) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Multiple active courses contain this quiz; use the course-scoped endpoint");
    }
    Enrollment enrollment =
        enrollments.stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You do not have access to this quiz"));
    curriculumAccessService.requireSectionAccess(
        quiz.getSection(), enrollment, Instant.now(), "Quiz");
    return enrollment;
  }

  public Enrollment requireActiveAttemptEnrollment(UUID userId, QuizAttempt attempt) {
    Enrollment enrollment = attempt.getEnrollment();
    boolean matchesUser = enrollment.getUser().getId().equals(userId);
    boolean matchesTemplate =
        enrollment
            .getCourse()
            .getTemplateVersion()
            .getId()
            .equals(attempt.getQuiz().getSection().getTemplateVersion().getId());
    if (!matchesUser
        || !matchesTemplate
        || enrollment.getStatus() != EnrollmentStatus.ACTIVE
        || curriculumAccessService.isEnrollmentExpired(enrollment, Instant.now())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You do not have access to this quiz attempt");
    }
    curriculumAccessService.requireSectionAccess(
        attempt.getQuiz().getSection(), enrollment, Instant.now(), "Quiz");
    return enrollment;
  }
}
