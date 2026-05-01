package com.gii.api.service.quiz;

import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.quiz.QuizRepository;
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

  public UUID requireCurrentUserId(Authentication authentication) {
    return currentUserService.getCurrentUserId(authentication);
  }

  public User requireCurrentUser(Authentication authentication) {
    return currentUserService.getCurrentUser(authentication);
  }

  public Quiz requirePublishedQuiz(UUID quizId) {
    return quizRepository
        .findByIdAndStatus(quizId, PublishStatus.PUBLISHED)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
  }

  public void ensureActiveEnrollment(UUID userId, UUID courseId) {
    boolean enrolled =
        enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
            userId, courseId, EnrollmentStatus.ACTIVE);
    if (!enrolled) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You do not have access to this quiz");
    }
  }
}
