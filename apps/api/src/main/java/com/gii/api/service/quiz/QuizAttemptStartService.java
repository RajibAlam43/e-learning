package com.gii.api.service.quiz;

import com.gii.api.model.response.quiz.QuizAttemptStartResponse;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.quiz.QuizAttempt;
import com.gii.common.entity.quiz.QuizQuestion;
import com.gii.common.entity.user.User;
import com.gii.common.repository.quiz.QuizAttemptRepository;
import com.gii.common.repository.quiz.QuizQuestionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizAttemptStartService {

  private final QuizAccessService quizAccessService;
  private final QuizAttemptRepository attemptRepository;
  private final QuizQuestionRepository questionRepository;

  public QuizAttemptStartResponse execute(UUID quizId, Authentication authentication) {
    User user = quizAccessService.requireCurrentUser(authentication);
    Quiz quiz = quizAccessService.requirePublishedQuiz(quizId);
    quizAccessService.ensureActiveEnrollment(user.getId(), quiz.getCourse().getId());

    long usedAttempts = attemptRepository.countByQuizIdAndUserId(quizId, user.getId());
    if (usedAttempts >= quiz.getMaxAttempts()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum attempts reached");
    }

    int attemptNo = (int) usedAttempts + 1;
    QuizAttempt attempt = QuizAttempt.builder().quiz(quiz).user(user).attemptNo(attemptNo).build();
    attemptRepository.save(attempt);

    List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByPositionAsc(quizId);
    int totalPoints = questions.stream().mapToInt(QuizQuestion::getPoints).sum();
    Instant deadline =
        quiz.getTimeLimitSec() != null
            ? attempt.getStartedAt().plusSeconds(quiz.getTimeLimitSec())
            : null;

    return QuizAttemptStartResponse.builder()
        .attemptId(attempt.getId())
        .attemptNumber(attemptNo)
        .startedAt(attempt.getStartedAt())
        .deadline(deadline)
        .timeRemainingSeconds(quiz.getTimeLimitSec() != null ? (long) quiz.getTimeLimitSec() : null)
        .timeLimitSec(quiz.getTimeLimitSec())
        .totalQuestions(questions.size())
        .totalPoints(totalPoints)
        .quizTitle(quiz.getTitle())
        .instructions(null)
        .build();
  }
}
