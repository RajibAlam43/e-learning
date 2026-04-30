package com.gii.api.service.quiz;

import com.gii.api.model.request.quiz.QuizAnswerSubmissionRequest;
import com.gii.api.model.request.quiz.SubmitQuizAttemptRequest;
import com.gii.api.model.response.quiz.QuizAttemptResultResponse;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.quiz.QuizAttempt;
import com.gii.common.entity.quiz.QuizAttemptAnswer;
import com.gii.common.entity.quiz.QuizAttemptAnswerId;
import com.gii.common.entity.quiz.QuizChoice;
import com.gii.common.entity.quiz.QuizQuestion;
import com.gii.common.repository.quiz.QuizAttemptAnswerRepository;
import com.gii.common.repository.quiz.QuizAttemptRepository;
import com.gii.common.repository.quiz.QuizChoiceRepository;
import com.gii.common.repository.quiz.QuizQuestionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizAttemptSubmitService {

  private final QuizAccessService quizAccessService;
  private final QuizAttemptRepository attemptRepository;
  private final QuizQuestionRepository questionRepository;
  private final QuizChoiceRepository choiceRepository;
  private final QuizAttemptAnswerRepository attemptAnswerRepository;
  private final QuizAttemptResultService attemptResultService;

  public QuizAttemptResultResponse execute(
      UUID attemptId, SubmitQuizAttemptRequest request, Authentication authentication) {
    UUID userId = quizAccessService.requireCurrentUserId(authentication);
    QuizAttempt attempt =
        attemptRepository
            .findByIdAndUserId(attemptId, userId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));

    if (attempt.getSubmittedAt() != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt already submitted");
    }

    Quiz quiz = attempt.getQuiz();
    quizAccessService.ensureActiveEnrollment(userId, quiz.getCourse().getId());

    if (quiz.getTimeLimitSec() != null) {
      Instant deadline = attempt.getStartedAt().plusSeconds(quiz.getTimeLimitSec());
      if (Instant.now().isAfter(deadline)) {
        throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT, "Time limit exceeded");
      }
    }

    // Reject duplicate question submissions to prevent ambiguity/injection.
    Set<UUID> questionIds = new HashSet<>();
    for (QuizAnswerSubmissionRequest answer : request.answers()) {
      if (!questionIds.add(answer.questionId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate answer for question");
      }
    }

    List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByPositionAsc(quiz.getId());
    Map<UUID, QuizQuestion> questionById =
        questions.stream().collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));
    Map<UUID, QuizChoice> choiceById =
        choiceRepository.findByQuestionIdIn(questionById.keySet().stream().toList()).stream()
            .collect(Collectors.toMap(QuizChoice::getId, Function.identity()));

    List<QuizAttemptAnswer> attemptAnswers = new ArrayList<>();
    int earnedPoints = 0;

    for (QuizAnswerSubmissionRequest submitted : request.answers()) {
      QuizQuestion question = questionById.get(submitted.questionId());
      if (question == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Answer contains invalid question");
      }

      QuizChoice choice = choiceById.get(submitted.choiceId());
      if (choice == null || !choice.getQuestion().getId().equals(question.getId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Answer choice does not belong to question");
      }

      attemptAnswers.add(
          QuizAttemptAnswer.builder()
              .attempt(attempt)
              .question(question)
              .choice(choice)
              .id(
                  QuizAttemptAnswerId.builder()
                      .attemptId(attempt.getId())
                      .questionId(question.getId())
                      .build())
              .build());

      if (Boolean.TRUE.equals(choice.getIsCorrect())) {
        earnedPoints += question.getPoints();
      }
    }

    attemptAnswerRepository.deleteByAttemptId(attempt.getId());
    attemptAnswerRepository.saveAll(attemptAnswers);

    final int totalPoints = questions.stream().mapToInt(QuizQuestion::getPoints).sum();
    int scorePct = totalPoints == 0 ? 0 : (int) Math.round((earnedPoints * 100.0) / totalPoints);
    boolean passed = scorePct >= quiz.getPassingScorePct();

    attempt.setScorePct(scorePct);
    attempt.setPassed(passed);
    attempt.setSubmittedAt(Instant.now());
    attemptRepository.save(attempt);

    return attemptResultService.execute(attempt.getId(), authentication);
  }
}
