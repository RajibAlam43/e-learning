package com.gii.api.controller;

import com.gii.api.model.request.quiz.SubmitQuizAttemptRequest;
import com.gii.api.model.response.quiz.QuizAttemptResultResponse;
import com.gii.api.model.response.quiz.QuizAttemptStartResponse;
import com.gii.api.model.response.quiz.QuizAttemptSummaryResponse;
import com.gii.api.model.response.quiz.QuizQuestionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class QuizApiController implements QuizApi {

    @Override
    public ResponseEntity<QuizQuestionsResponse> getQuizQuestions(UUID quizId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<QuizAttemptStartResponse> startQuizAttempt(UUID quizId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<QuizAttemptResultResponse> submitQuizAttempt(UUID attemptId, SubmitQuizAttemptRequest request, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<List<QuizAttemptSummaryResponse>> getQuizAttempts(UUID quizId, Authentication authentication) {
        return null;
    }

    @Override
    public ResponseEntity<QuizAttemptResultResponse> getQuizAttemptResult(UUID attemptId, Authentication authentication) {
        return null;
    }
}
