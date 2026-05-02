# QuizApi Test Plan

## GET /learn/quizzes/{quizId}
### A. What this API should accomplish
- Return quiz questions and student-attempt metadata for a published quiz the learner can access.
### B. Steps this API should follow
1. Resolve current user id from authentication.
2. Lookup quiz by id with `status=PUBLISHED`.
3. Validate active enrollment for quiz course.
4. Lookup questions by `quizId` ordered by question position.
5. Lookup all choices for those questions.
6. Lookup prior attempts for current user+quiz.
7. Compute `totalAttempts`, `remainingAttempts`, `bestScorePct`, `canRetry`.
8. Return question payload without exposing `isCorrect` before submission.
### C. API tests
- returns ordered questions, choices, and attempt metadata.
- does not expose correctness flags in question choices.
### D. DataJpa tests
- `findByIdAndStatus`.
- `findByQuizIdOrderByPositionAsc`.
- `findByQuestionIdIn`.
### E. Important/Critical validations to include
- block non-enrolled users.
- never leak answer correctness pre-submit.

## POST /learn/quizzes/{quizId}/attempts
### A. What this API should accomplish
- Start a new attempt only when user has access and attempt limit is not exhausted.
### B. Steps this API should follow
1. Resolve current user entity.
2. Lookup published quiz.
3. Validate active enrollment for quiz course.
4. Count attempts by `(quizId,userId)`.
5. Reject when count >= `maxAttempts`.
6. Create attempt row with incremented `attemptNo`.
7. Lookup question set and calculate total points.
8. Compute deadline/time remaining from `timeLimitSec`.
9. Return attempt start response.
### C. API tests
- creates attempt #1 with timing info and total question stats.
- returns 400 when max attempts reached.
### D. DataJpa tests
- `countByQuizIdAndUserId`.
- `findByQuizIdAndUserIdOrderByAttemptNoDesc`.
### E. Important/Critical validations to include
- enforce max attempts at write-time.

## POST /learn/quiz-attempts/{attemptId}/submit
### A. What this API should accomplish
- Validate and grade a quiz submission, persist answers, mark attempt as submitted.
### B. Steps this API should follow
1. Resolve current user id.
2. Lookup attempt by `(attemptId,userId)`.
3. Reject if already submitted.
4. Validate active enrollment for attempt’s quiz course.
5. Enforce time limit when quiz has `timeLimitSec`.
6. Validate no duplicate `questionId` entries in request.
7. Lookup quiz questions and available choices.
8. Validate each answer references existing question and matching choice.
9. Replace existing attempt answers for this attempt.
10. Compute earned points, total points, score percentage, pass/fail.
11. Set `submittedAt`, `scorePct`, `passed` on attempt and save.
12. Return full attempt result response.
### C. API tests
- successful submit persists attempt answers and grading.
- returns 408 when attempt deadline exceeded.
- returns 400 for duplicate question submissions.
- contract-gap: empty `answers` should return 400 validation error.
### D. DataJpa tests
- `findByIdAndUserId`.
- `findByAttemptId` and `deleteByAttemptId`.
### E. Important/Critical validations to include
- ownership gate (`attemptId` must belong to current user).
- reject cross-question choice injection.

## GET /learn/quizzes/{quizId}/attempts
### A. What this API should accomplish
- Return student’s attempt history for a quiz with computed status and derived metrics.
### B. Steps this API should follow
1. Resolve current user id.
2. Lookup published quiz.
3. Validate active enrollment for quiz course.
4. Lookup quiz questions for total points.
5. Lookup attempts for `(quizId,userId)` in attempt-no descending order.
6. Map each attempt to summary (earned points, duration, status, result URL).
### C. API tests
- returns graded history with expected status and ordering.
### D. DataJpa tests
- attempt ordering query by quiz+user.
### E. Important/Critical validations to include
- no cross-user attempt leakage.

## GET /learn/quiz-attempts/{attemptId}
### A. What this API should accomplish
- Return full attempt result with per-question correctness and guidance.
### B. Steps this API should follow
1. Resolve current user id.
2. Lookup attempt by `(attemptId,userId)`.
3. Validate active enrollment for attempt’s quiz course.
4. Lookup questions for quiz.
5. Lookup all choices for those questions.
6. Lookup submitted answers for attempt.
7. Compute per-question correctness, earned points, total score.
8. Compute retry metadata (`totalAttempts`, `bestScorePct`, `canRetry`).
9. Return result payload with next action.
### C. API tests
- returns complete result payload after submission.
### D. DataJpa tests
- answer retrieval by attempt id.
- attempt count and history queries used for retry metadata.
### E. Important/Critical validations to include
- attempt must be owned by current user.

