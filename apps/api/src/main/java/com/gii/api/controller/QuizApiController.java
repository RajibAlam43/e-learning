package com.gii.api.controller;

import com.gii.api.model.request.SubmitQuizAttemptRequest;
import com.gii.api.model.response.QuizAttemptResultResponse;
import com.gii.api.model.response.QuizAttemptStartResponse;
import com.gii.api.model.response.QuizAttemptSummaryResponse;
import com.gii.api.model.response.QuizQuestionsResponse;
import com.gii.api.processor.QuizApiProcessingService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/learn")
public class QuizApiController {

    private final QuizApiProcessingService quizApiProcessingService;

    /**
     * Returns quiz questions for the current student after validating access.
     *
     * @param quizId ID of the quiz to load
     * @param authentication authenticated user context from Spring Security
     * @return quiz questions payload
     */
    @GetMapping("/quizzes/{quizId}")
    public ResponseEntity<@NotNull QuizQuestionsResponse> getQuizQuestions(
            @PathVariable UUID quizId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizApiProcessingService.getQuizQuestions(quizId, authentication));
    }

    /**
     * Starts a new quiz attempt for the current student.
     *
     * @param quizId ID of the quiz to attempt
     * @param authentication authenticated user context from Spring Security
     * @return created attempt details (for example attemptId and start metadata)
     */
    @PostMapping("/quizzes/{quizId}/attempts")
    public ResponseEntity<@NotNull QuizAttemptStartResponse> startQuizAttempt(
            @PathVariable UUID quizId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizApiProcessingService.startQuizAttempt(quizId, authentication));
    }

    /**
     * Submits answers for an existing quiz attempt and triggers scoring.
     *
     * @param attemptId ID of the quiz attempt being submitted
     * @param request submitted answers payload
     * @param authentication authenticated user context from Spring Security
     * @return evaluated attempt result including score/outcome
     */
    @PostMapping("/quiz-attempts/{attemptId}/submit")
    public ResponseEntity<@NotNull QuizAttemptResultResponse> submitQuizAttempt(
            @PathVariable UUID attemptId,
            @RequestBody SubmitQuizAttemptRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizApiProcessingService.submitQuizAttempt(attemptId, request, authentication));
    }

    /**
     * Lists all attempts for a quiz made by the current student.
     *
     * @param quizId ID of the quiz
     * @param authentication authenticated user context from Spring Security
     * @return list of attempt summaries for the quiz
     */
    @GetMapping("/quizzes/{quizId}/attempts")
    public ResponseEntity<@NotNull List<QuizAttemptSummaryResponse>> getQuizAttempts(
            @PathVariable UUID quizId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizApiProcessingService.getQuizAttempts(quizId, authentication));
    }

    /**
     * Returns result details for a specific quiz attempt.
     *
     * @param attemptId ID of the quiz attempt
     * @param authentication authenticated user context from Spring Security
     * @return quiz attempt result payload
     */
    @GetMapping("/quiz-attempts/{attemptId}")
    public ResponseEntity<@NotNull QuizAttemptResultResponse> getQuizAttemptResult(
            @PathVariable UUID attemptId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizApiProcessingService.getQuizAttemptResult(attemptId, authentication));
    }
}
