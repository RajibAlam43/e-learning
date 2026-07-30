package com.gii.api.service.quiz;

import com.gii.api.model.response.quiz.QuizChoiceResponse;
import com.gii.api.model.response.quiz.QuizQuestionResponse;
import com.gii.api.model.response.quiz.QuizQuestionsResponse;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.quiz.QuizAttempt;
import com.gii.common.entity.quiz.QuizChoice;
import com.gii.common.entity.quiz.QuizQuestion;
import com.gii.common.repository.quiz.QuizAttemptRepository;
import com.gii.common.repository.quiz.QuizChoiceRepository;
import com.gii.common.repository.quiz.QuizQuestionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizQuestionsService {

  private final QuizAccessService quizAccessService;
  private final QuizQuestionRepository questionRepository;
  private final QuizChoiceRepository choiceRepository;
  private final QuizAttemptRepository attemptRepository;
  private final LocalizedContentService localizedContentService;

  public QuizQuestionsResponse execute(UUID quizId, Authentication authentication) {
    UUID userId = quizAccessService.requireCurrentUserId(authentication);
    Quiz quiz = quizAccessService.requirePublishedQuiz(quizId);
    quizAccessService.ensureActiveEnrollment(userId, quiz.getCourse().getId());

    List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByPositionAsc(quizId);
    Map<UUID, List<QuizChoice>> choicesByQuestion =
        choiceRepository
            .findByQuestionIdIn(questions.stream().map(QuizQuestion::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(choice -> choice.getQuestion().getId()));

    List<QuizAttempt> attempts =
        attemptRepository.findByQuizIdAndUserIdOrderByAttemptNoDesc(quizId, userId);
    int totalAttempts = attempts.size();
    int remainingAttempts = Math.max(quiz.getMaxAttempts() - totalAttempts, 0);
    Integer bestScore =
        attempts.stream()
            .map(QuizAttempt::getScorePct)
            .filter(java.util.Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(null);

    List<QuizQuestionResponse> questionResponses =
        questions.stream()
            .map(
                question ->
                    QuizQuestionResponse.builder()
                        .questionId(question.getId())
                        .position(question.getPosition())
                        .questionText(
                            localizedContentService.text(
                                question.getQuestionText(), question.getQuestionTextEn()))
                        .questionType(question.getQuestionType())
                        .points(question.getPoints())
                        .choices(
                            choicesByQuestion.getOrDefault(question.getId(), List.of()).stream()
                                .sorted(Comparator.comparing(QuizChoice::getChoiceText))
                                // Never expose correctness before submission.
                                .map(
                                    choice ->
                                        QuizChoiceResponse.builder()
                                            .choiceId(choice.getId())
                                            .choiceText(
                                                localizedContentService.text(
                                                    choice.getChoiceText(),
                                                    choice.getChoiceTextEn()))
                                            .isCorrect(null)
                                            .build())
                                .toList())
                        .showAsShuffled(false)
                        .build())
            .toList();

    return QuizQuestionsResponse.builder()
        .quizId(quiz.getId())
        .quizTitle(localizedContentService.text(quiz.getTitle(), quiz.getTitleEn()))
        .passingScorePct(quiz.getPassingScorePct())
        .maxAttempts(quiz.getMaxAttempts())
        .timeLimitSec(quiz.getTimeLimitSec())
        .totalAttempts(totalAttempts)
        .remainingAttempts(remainingAttempts)
        .canRetry(remainingAttempts > 0)
        .bestScorePct(bestScore)
        .questions(questionResponses)
        .instructions(null)
        .shuffleQuestions(false)
        .showCorrectAnswers(false)
        .build();
  }
}
