package com.gii.api.service.quiz;

import com.gii.api.model.response.quiz.QuizAttemptQuestionResultResponse;
import com.gii.api.model.response.quiz.QuizAttemptResultResponse;
import com.gii.api.model.response.quiz.QuizResultChoiceResponse;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.quiz.QuizAttempt;
import com.gii.common.entity.quiz.QuizAttemptAnswer;
import com.gii.common.entity.quiz.QuizChoice;
import com.gii.common.entity.quiz.QuizQuestion;
import com.gii.common.entity.user.User;
import com.gii.common.repository.quiz.QuizAttemptAnswerRepository;
import com.gii.common.repository.quiz.QuizAttemptRepository;
import com.gii.common.repository.quiz.QuizChoiceRepository;
import com.gii.common.repository.quiz.QuizQuestionRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
@Transactional(readOnly = true)
public class QuizAttemptResultService {

  private final QuizAccessService quizAccessService;
  private final QuizAttemptRepository attemptRepository;
  private final QuizQuestionRepository questionRepository;
  private final QuizChoiceRepository choiceRepository;
  private final QuizAttemptAnswerRepository answerRepository;

  public QuizAttemptResultResponse execute(UUID attemptId, Authentication authentication) {
    User user = quizAccessService.requireCurrentUser(authentication);
    QuizAttempt attempt =
        attemptRepository
            .findByIdAndUserId(attemptId, user.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));

    Quiz quiz = attempt.getQuiz();
    quizAccessService.ensureActiveEnrollment(user.getId(), quiz.getCourse().getId());

    List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByPositionAsc(quiz.getId());
    Map<UUID, QuizQuestion> questionById =
        questions.stream().collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));
    Map<UUID, List<QuizChoice>> choicesByQuestionId =
        choiceRepository.findByQuestionIdIn(questionById.keySet().stream().toList()).stream()
            .collect(Collectors.groupingBy(choice -> choice.getQuestion().getId()));
    Map<UUID, QuizAttemptAnswer> answerByQuestionId =
        answerRepository.findByAttemptId(attemptId).stream()
            .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), Function.identity()));

    int totalPoints = questions.stream().mapToInt(QuizQuestion::getPoints).sum();
    int earnedPoints = 0;
    List<QuizAttemptQuestionResultResponse> questionResults = new ArrayList<>();

    for (QuizQuestion question : questions) {
      List<QuizChoice> choices = choicesByQuestionId.getOrDefault(question.getId(), List.of());
      QuizChoice correct =
          choices.stream()
              .filter(choice -> Boolean.TRUE.equals(choice.getIsCorrect()))
              .findFirst()
              .orElse(null);
      QuizAttemptAnswer userAnswer = answerByQuestionId.get(question.getId());
      QuizChoice userChoice = userAnswer != null ? userAnswer.getChoice() : null;
      boolean correctAnswer =
          userChoice != null && correct != null && userChoice.getId().equals(correct.getId());
      int earned = correctAnswer ? question.getPoints() : 0;
      earnedPoints += earned;

      List<QuizResultChoiceResponse> allChoices =
          choices.stream()
              .map(
                  choice ->
                      QuizResultChoiceResponse.builder()
                          .choiceId(choice.getId())
                          .choiceText(choice.getChoiceText())
                          .isCorrect(choice.getIsCorrect())
                          .wasUserChoice(
                              userChoice != null && userChoice.getId().equals(choice.getId()))
                          .build())
              .toList();

      questionResults.add(
          QuizAttemptQuestionResultResponse.builder()
              .questionId(question.getId())
              .position(question.getPosition())
              .questionText(question.getQuestionText())
              .points(question.getPoints())
              .userChoiceId(userChoice != null ? userChoice.getId() : null)
              .userChoiceText(userChoice != null ? userChoice.getChoiceText() : null)
              .userAnswerCorrect(correctAnswer)
              .correctChoiceId(correct != null ? correct.getId() : null)
              .correctChoiceText(correct != null ? correct.getChoiceText() : null)
              .allChoices(allChoices)
              .explanation(question.getExplanationText())
              .earnedPoints(earned)
              .feedbackMessage(correctAnswer ? "Correct!" : "Incorrect.")
              .build());
    }

    int scorePct = totalPoints == 0 ? 0 : (int) Math.round((earnedPoints * 100.0) / totalPoints);
    int totalAttempts = (int) attemptRepository.countByQuizIdAndUserId(quiz.getId(), user.getId());
    int bestScore =
        attemptRepository
            .findByQuizIdAndUserIdOrderByAttemptNoDesc(quiz.getId(), user.getId())
            .stream()
            .map(QuizAttempt::getScorePct)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(scorePct);
    boolean canRetry = totalAttempts < quiz.getMaxAttempts();
    Long duration =
        attempt.getSubmittedAt() == null
            ? null
            : Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).toSeconds();

    return QuizAttemptResultResponse.builder()
        .attemptId(attempt.getId())
        .quizId(quiz.getId())
        .quizTitle(quiz.getTitle())
        .attemptNumber(attempt.getAttemptNo())
        .scorePct(attempt.getScorePct() != null ? attempt.getScorePct() : scorePct)
        .totalPoints(totalPoints)
        .earnedPoints(earnedPoints)
        .passingScorePct(quiz.getPassingScorePct())
        .passed(Boolean.TRUE.equals(attempt.getPassed()))
        .startedAt(attempt.getStartedAt())
        .submittedAt(attempt.getSubmittedAt())
        .durationSeconds(duration)
        .questionResults(questionResults)
        .totalAttempts(totalAttempts)
        .canRetry(canRetry)
        .bestScorePct(bestScore)
        .feedbackMessage(
            Boolean.TRUE.equals(attempt.getPassed())
                ? "Congratulations! You passed the quiz."
                : "Keep trying. You can improve your score.")
        .nextAction(
            Boolean.TRUE.equals(attempt.getPassed())
                ? "COMPLETE_LESSON"
                : (canRetry ? "RETRY_QUIZ" : "CONTINUE"))
        .build();
  }
}
