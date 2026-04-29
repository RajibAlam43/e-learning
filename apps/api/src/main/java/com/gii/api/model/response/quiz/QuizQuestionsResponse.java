package com.gii.api.model.response.quiz;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record QuizQuestionsResponse(
        UUID quizId,
        String quizTitle,
        
        // Quiz configuration
        Integer passingScorePct,
        Integer maxAttempts,
        Integer timeLimitSec,  // Null if no time limit
        
        // User's attempt history
        Integer totalAttempts,
        Integer remainingAttempts,
        Boolean canRetry,  // Whether user can start new attempt
        Integer bestScorePct,  // Best score across all attempts
        
        // Questions
        List<QuizQuestionResponse> questions,
        
        // UI hints
        String instructions,  // Optional quiz instructions
        Boolean shuffleQuestions,  // Whether questions are randomized
        Boolean showCorrectAnswers  // After submission or only at end
) {}