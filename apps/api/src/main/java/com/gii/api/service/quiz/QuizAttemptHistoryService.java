package com.gii.api.service.quiz;

import com.gii.api.model.response.quiz.QuizAttemptSummaryResponse;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.quiz.QuizAttempt;
import com.gii.common.entity.quiz.QuizQuestion;
import com.gii.common.repository.quiz.QuizAttemptRepository;
import com.gii.common.repository.quiz.QuizQuestionRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizAttemptHistoryService {

  private final QuizAccessService quizAccessService;
  private final QuizAttemptRepository attemptRepository;
  private final QuizQuestionRepository questionRepository;

  public List<QuizAttemptSummaryResponse> execute(UUID quizId, Authentication authentication) {
    UUID userId = quizAccessService.requireCurrentUserId(authentication);
    Quiz quiz = quizAccessService.requirePublishedQuiz(quizId);
    quizAccessService.ensureActiveEnrollment(userId, quiz.getCourse().getId());

    List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByPositionAsc(quizId);
    int totalPoints = questions.stream().mapToInt(QuizQuestion::getPoints).sum();

    List<QuizAttempt> attempts =
        attemptRepository.findByQuizIdAndUserIdOrderByAttemptNoDesc(quizId, userId);
    return attempts.stream()
        .map(
            attempt -> {
              int earnedPoints =
                  attempt.getScorePct() == null
                      ? 0
                      : (int) Math.round((attempt.getScorePct() / 100.0) * totalPoints);
              Long durationSec =
                  attempt.getSubmittedAt() == null
                      ? null
                      : Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt())
                          .toSeconds();
              String status = attempt.getSubmittedAt() == null ? "IN_PROGRESS" : "GRADED";

              return QuizAttemptSummaryResponse.builder()
                  .attemptId(attempt.getId())
                  .attemptNumber(attempt.getAttemptNo())
                  .scorePct(attempt.getScorePct())
                  .passed(attempt.getPassed())
                  .totalPoints(totalPoints)
                  .earnedPoints(earnedPoints)
                  .startedAt(attempt.getStartedAt())
                  .submittedAt(attempt.getSubmittedAt())
                  .durationSeconds(durationSec)
                  .status(status)
                  .resultUrl("/learn/quiz-attempts/" + attempt.getId())
                  .build();
            })
        .toList();
  }
}
