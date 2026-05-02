package com.gii.api.controller;

import com.gii.api.model.request.quiz.SubmitQuizAttemptRequest;
import com.gii.api.model.response.quiz.QuizAttemptResultResponse;
import com.gii.api.model.response.quiz.QuizAttemptStartResponse;
import com.gii.api.model.response.quiz.QuizAttemptSummaryResponse;
import com.gii.api.model.response.quiz.QuizQuestionsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Quizzes", description = "Quiz creation, attempts, and grading")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/learn")
@PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
public interface QuizApi {

  @GetMapping("/quizzes/{quizId}")
  @Operation(
      summary = "Get quiz questions",
      description = "Fetch quiz questions and choices after validating student access to the quiz.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Quiz questions retrieved",
            content = @Content(schema = @Schema(implementation = QuizQuestionsResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Access denied - max attempts exceeded"),
        @ApiResponse(responseCode = "404", description = "Quiz not found")
      })
  ResponseEntity<QuizQuestionsResponse> getQuizQuestions(
      @PathVariable UUID quizId, Authentication authentication);

  @PostMapping("/quizzes/{quizId}/attempts")
  @Operation(
      summary = "Start quiz attempt",
      description = "Start a new quiz attempt. Returns attempt ID and timing information.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Attempt started",
            content = @Content(schema = @Schema(implementation = QuizAttemptStartResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot start attempt - max attempts reached"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Quiz not found")
      })
  ResponseEntity<QuizAttemptStartResponse> startQuizAttempt(
      @PathVariable UUID quizId, Authentication authentication);

  @PostMapping("/quiz-attempts/{attemptId}/submit")
  @Operation(
      summary = "Submit quiz attempt",
      description = "Submit answers for a quiz attempt. Triggers automatic scoring and feedback.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Attempt submitted and graded",
            content = @Content(schema = @Schema(implementation = QuizAttemptResultResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid attempt or submission data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Attempt not found"),
        @ApiResponse(responseCode = "408", description = "Time limit exceeded")
      })
  ResponseEntity<QuizAttemptResultResponse> submitQuizAttempt(
      @PathVariable UUID attemptId,
      @Valid @RequestBody SubmitQuizAttemptRequest request,
      Authentication authentication);

  @GetMapping("/quizzes/{quizId}/attempts")
  @Operation(
      summary = "List quiz attempts",
      description = "Get all quiz attempts made by the current student.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Attempts retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Quiz not found")
      })
  ResponseEntity<List<QuizAttemptSummaryResponse>> getQuizAttempts(
      @PathVariable UUID quizId, Authentication authentication);

  @GetMapping("/quiz-attempts/{attemptId}")
  @Operation(
      summary = "Get attempt result",
      description = "Get detailed result and feedback for a specific quiz attempt.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Result retrieved",
            content = @Content(schema = @Schema(implementation = QuizAttemptResultResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Attempt not found")
      })
  ResponseEntity<QuizAttemptResultResponse> getQuizAttemptResult(
      @PathVariable UUID attemptId, Authentication authentication);
}
