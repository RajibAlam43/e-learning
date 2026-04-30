package com.gii.api.controller;

import com.gii.api.model.request.quiz.SubmitQuizAttemptRequest;
import com.gii.api.model.response.quiz.QuizAttemptResultResponse;
import com.gii.api.model.response.quiz.QuizAttemptStartResponse;
import com.gii.api.model.response.quiz.QuizAttemptSummaryResponse;
import com.gii.api.model.response.quiz.QuizQuestionsResponse;
import com.gii.api.service.quiz.QuizAttemptHistoryService;
import com.gii.api.service.quiz.QuizAttemptResultService;
import com.gii.api.service.quiz.QuizAttemptStartService;
import com.gii.api.service.quiz.QuizAttemptSubmitService;
import com.gii.api.service.quiz.QuizQuestionsService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuizApiController implements QuizApi {

  private final QuizQuestionsService quizQuestionsService;
  private final QuizAttemptStartService quizAttemptStartService;
  private final QuizAttemptSubmitService quizAttemptSubmitService;
  private final QuizAttemptHistoryService quizAttemptHistoryService;
  private final QuizAttemptResultService quizAttemptResultService;

  @Override
  public ResponseEntity<QuizQuestionsResponse> getQuizQuestions(
      UUID quizId, Authentication authentication) {
    return ResponseEntity.ok(quizQuestionsService.execute(quizId, authentication));
  }

  @Override
  public ResponseEntity<QuizAttemptStartResponse> startQuizAttempt(
      UUID quizId, Authentication authentication) {
    return ResponseEntity.ok(quizAttemptStartService.execute(quizId, authentication));
  }

  @Override
  public ResponseEntity<QuizAttemptResultResponse> submitQuizAttempt(
      UUID attemptId, SubmitQuizAttemptRequest request, Authentication authentication) {
    return ResponseEntity.ok(quizAttemptSubmitService.execute(attemptId, request, authentication));
  }

  @Override
  public ResponseEntity<List<QuizAttemptSummaryResponse>> getQuizAttempts(
      UUID quizId, Authentication authentication) {
    return ResponseEntity.ok(quizAttemptHistoryService.execute(quizId, authentication));
  }

  @Override
  public ResponseEntity<QuizAttemptResultResponse> getQuizAttemptResult(
      UUID attemptId, Authentication authentication) {
    return ResponseEntity.ok(quizAttemptResultService.execute(attemptId, authentication));
  }
}
