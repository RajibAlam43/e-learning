package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateQuizChoiceRequest;
import com.gii.api.model.request.admin.CreateQuizQuestionRequest;
import com.gii.api.model.request.admin.CreateQuizRequest;
import com.gii.api.model.request.admin.UpdateQuizChoiceRequest;
import com.gii.api.model.request.admin.UpdateQuizQuestionRequest;
import com.gii.api.model.request.admin.UpdateQuizRequest;
import com.gii.api.model.response.admin.AdminQuizChoiceResponse;
import com.gii.api.model.response.admin.AdminQuizDetailResponse;
import com.gii.api.model.response.admin.AdminQuizQuestionResponse;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.quiz.QuizChoice;
import com.gii.common.entity.quiz.QuizQuestion;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.QuestionType;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.quiz.QuizChoiceRepository;
import com.gii.common.repository.quiz.QuizQuestionRepository;
import com.gii.common.repository.quiz.QuizRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminQuizManagementService {

  private final CourseRepository courseRepository;
  private final QuizRepository quizRepository;
  private final QuizQuestionRepository questionRepository;
  private final QuizChoiceRepository choiceRepository;

  public AdminQuizDetailResponse create(UUID courseId, CreateQuizRequest request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    Quiz quiz =
        Quiz.builder()
            .course(course)
            .title(request.title().trim())
            .passingScorePct(request.passingScorePct())
            .maxAttempts(request.maxAttempts())
            .timeLimitSec(request.timeLimitSec())
            .status(PublishStatus.DRAFT)
            .build();
    Quiz savedQuiz = quizRepository.save(quiz);
    createQuestions(savedQuiz, request.questions());
    return toDetail(savedQuiz.getId());
  }

  public AdminQuizDetailResponse update(UUID quizId, UpdateQuizRequest request) {
    Quiz quiz =
        quizRepository
            .findById(quizId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
    if (request.title() != null && !request.title().isBlank()) {
      quiz.setTitle(request.title().trim());
    }
    if (request.passingScorePct() != null) {
      quiz.setPassingScorePct(request.passingScorePct());
    }
    if (request.maxAttempts() != null) {
      quiz.setMaxAttempts(request.maxAttempts());
    }
    if (request.timeLimitSec() != null) {
      quiz.setTimeLimitSec(request.timeLimitSec());
    }
    quizRepository.save(quiz);

    if (request.questions() != null) {
      replaceQuestions(quiz, request.questions());
    }
    return toDetail(quiz.getId());
  }

  public void publish(UUID quizId) {
    Quiz quiz =
        quizRepository
            .findById(quizId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
    List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByPositionAsc(quizId);
    if (questions.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Quiz must have at least one question");
    }
    for (QuizQuestion question : questions) {
      long correctCount =
          choiceRepository.findByQuestionId(question.getId()).stream()
              .filter(choice -> Boolean.TRUE.equals(choice.getIsCorrect()))
              .count();
      if (correctCount == 0) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Every question needs at least one correct choice");
      }
    }
    quiz.setStatus(PublishStatus.PUBLISHED);
    quizRepository.save(quiz);
  }

  private void createQuestions(Quiz quiz, List<CreateQuizQuestionRequest> questionRequests) {
    for (CreateQuizQuestionRequest request : questionRequests) {
      QuizQuestion question =
          QuizQuestion.builder()
              .quiz(quiz)
              .position(request.position())
              .questionText(request.questionText())
              .questionType(parseQuestionType(request.questionType()))
              .points(request.points())
              .explanationText(request.explanationText())
              .build();
      QuizQuestion savedQuestion = questionRepository.save(question);
      createChoices(savedQuestion, request.choices());
    }
  }

  private void createChoices(QuizQuestion question, List<CreateQuizChoiceRequest> choices) {
    for (CreateQuizChoiceRequest request : choices) {
      QuizChoice choice =
          QuizChoice.builder()
              .question(question)
              .choiceText(request.choiceText())
              .isCorrect(request.isCorrect())
              .build();
      choiceRepository.save(choice);
    }
  }

  private void replaceQuestions(Quiz quiz, List<UpdateQuizQuestionRequest> questionRequests) {
    List<QuizQuestion> existingQuestions =
        questionRepository.findByQuizIdOrderByPositionAsc(quiz.getId());
    for (QuizQuestion existing : existingQuestions) {
      choiceRepository.deleteAll(choiceRepository.findByQuestionId(existing.getId()));
    }
    questionRepository.deleteAll(existingQuestions);

    for (UpdateQuizQuestionRequest request : questionRequests) {
      QuizQuestion question =
          QuizQuestion.builder()
              .quiz(quiz)
              .position(request.position() == null ? 0 : request.position())
              .questionText(request.questionText())
              .questionType(parseQuestionType(request.questionType()))
              .points(request.points() == null ? 1 : request.points())
              .explanationText(request.explanationText())
              .build();
      QuizQuestion savedQuestion = questionRepository.save(question);
      if (request.choices() != null) {
        for (UpdateQuizChoiceRequest choiceRequest : request.choices()) {
          QuizChoice choice =
              QuizChoice.builder()
                  .question(savedQuestion)
                  .choiceText(choiceRequest.choiceText())
                  .isCorrect(choiceRequest.isCorrect() != null && choiceRequest.isCorrect())
                  .build();
          choiceRepository.save(choice);
        }
      }
    }
  }

  private AdminQuizDetailResponse toDetail(UUID quizId) {
    Quiz quiz =
        quizRepository
            .findById(quizId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
    List<AdminQuizQuestionResponse> questions =
        questionRepository.findByQuizIdOrderByPositionAsc(quizId).stream()
            .map(this::toQuestionResponse)
            .toList();
    return AdminQuizDetailResponse.builder()
        .quizId(quiz.getId())
        .title(quiz.getTitle())
        .passingScorePct(quiz.getPassingScorePct())
        .maxAttempts(quiz.getMaxAttempts())
        .timeLimitSec(quiz.getTimeLimitSec())
        .status(quiz.getStatus().name())
        .createdAt(quiz.getCreatedAt())
        .updatedAt(quiz.getUpdatedAt())
        .questions(questions)
        .build();
  }

  private AdminQuizQuestionResponse toQuestionResponse(QuizQuestion question) {
    List<AdminQuizChoiceResponse> choices =
        choiceRepository.findByQuestionId(question.getId()).stream()
            .map(
                choice ->
                    AdminQuizChoiceResponse.builder()
                        .choiceId(choice.getId())
                        .choiceText(choice.getChoiceText())
                        .isCorrect(choice.getIsCorrect())
                        .build())
            .toList();
    return AdminQuizQuestionResponse.builder()
        .questionId(question.getId())
        .position(question.getPosition())
        .questionText(question.getQuestionText())
        .questionType(question.getQuestionType().name())
        .points(question.getPoints())
        .explanationText(question.getExplanationText())
        .choices(choices)
        .build();
  }

  private QuestionType parseQuestionType(String value) {
    if (value == null || value.isBlank()) {
      return QuestionType.MCQ;
    }
    try {
      return QuestionType.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid question type");
    }
  }
}
