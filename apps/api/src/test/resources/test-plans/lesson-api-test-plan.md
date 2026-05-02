# LessonApi Test Plan

## GET /learn/lessons/{lessonId}
### A. What this API should accomplish
- Return lesson content package (lesson metadata, playback metadata, progress snapshot, resources) only for authorized/eligible learner.
### B. Steps this API should follow
1. Resolve current user id.
2. Lookup lesson by `lessonId`.
3. Reject with 404 when lesson does not exist or is not `PUBLISHED`.
4. Lookup ACTIVE enrollment for `(userId, lesson.courseId)`.
5. Evaluate lesson accessibility gate:
6. `isFree=true` grants access.
7. reject when enrollment expired.
8. if `releaseType=FIXED_DATE`, allow only when now >= `releaseAt`.
9. if `releaseType=RELATIVE_DAYS`, allow only when now >= `enrolledAt + unlockAfterDays`.
10. Lookup existing lesson progress row `(userId, lessonId)`; fallback defaults when absent.
11. Lookup media asset by `lessonId`; include only when media exists and status is `READY`.
12. Lookup lesson resources ordered by `position ASC`.
13. Return `LessonContentResponse`.
### C. API tests
- returns lesson content with progress + ordered resources for active enrolled student.
- returns 403 for non-enrolled user.
- returns 403 for release date not reached.
### D. DataJpa tests
- lesson progress lookup by composite key.
- resource ordering by lesson and position.
### E. Important/Critical validations to include
- enforce ownership/enrollment boundary for every lesson fetch.
- never expose non-ready media as playable payload.

## GET /learn/lessons/{lessonId}/playback
### A. What this API should accomplish
- Return provider-specific playback payload only when lesson/media exist, media is playable, and learner can access paid content.
### B. Steps this API should follow
1. Lookup lesson by `lessonId`.
2. Lookup media asset by `lessonId`.
3. Reject when media status is not `READY`.
4. For paid lesson (`isFree=false`), resolve current user and verify enrollment access for the lesson.
5. Route media through provider strategy (MUX/BUNNY/YOUTUBE) and return playback DTO.
### C. API tests
- returns playback payload for accessible lesson.
- contract-gap: unknown lesson should map to 404.
- contract-gap: non-ready media should map to 404 (or explicit contract status).
### D. DataJpa tests
- media lookup by lesson id via repository-backed integration path.
### E. Important/Critical validations to include
- strict access gate for paid lessons before issuing playback URL/token.

## POST /learn/lessons/{lessonId}/progress
### A. What this API should accomplish
- Upsert learner progress (last position and completion flag) for an accessible lesson.
### B. Steps this API should follow
1. Resolve current user.
2. Lookup published lesson.
3. Validate active enrollment for lesson course.
4. Validate lesson accessibility window/release rules.
5. Lookup progress by `(userId, lessonId)`, create row if missing.
6. If `lastPositionSec` present, update value.
7. If `completed=true`, set `completedAt=now`.
8. Persist row.
### C. API tests
- creates new progress row for first save.
- updates `lastPositionSec` on next save.
- contract-gap: negative `lastPositionSec` should be 400.
### D. DataJpa tests
- completion count query by user+course.
- latest learning activity query by user.
### E. Important/Critical validations to include
- guard against invalid progress payloads (negative position, malformed body).

## POST /learn/lessons/{lessonId}/complete
### A. What this API should accomplish
- Mark lesson as completed for an eligible learner.
### B. Steps this API should follow
1. Resolve current user.
2. Lookup published lesson.
3. Validate active enrollment for lesson course.
4. Validate lesson accessibility window/release rules.
5. Lookup/create progress row.
6. Set `completedAt=now`.
7. Persist row.
### C. API tests
- sets `completedAt` and leaves row persisted.
### D. DataJpa tests
- progress row persists with completion timestamp.
### E. Important/Critical validations to include
- idempotent behavior when already completed.

## GET /learn/courses/{courseId}/progress
### A. What this API should accomplish
- Return course-level and section-level completion summary for current learner.
### B. Steps this API should follow
1. Resolve current user id.
2. Validate ACTIVE enrollment for `(userId, courseId)` else 404.
3. Lookup published lessons for course ordered by position.
4. Lookup user lesson progress rows for that course.
5. Lookup published sections ordered by position.
6. Group lessons by section.
7. Compute per-section totals/completed/percentage.
8. Compute course totals/completed/pending/percentage.
9. Return `CourseProgressResponse`.
### C. API tests
- returns correct total/completed/pending and section aggregation.
### D. DataJpa tests
- published lesson query with media join + ordering.
- progress rows by user+course.
### E. Important/Critical validations to include
- completion denominator must be published lessons only.

## GET /learn/lessons/{lessonId}/resources
### A. What this API should accomplish
- Return ordered resource metadata list for accessible lesson.
### B. Steps this API should follow
1. Resolve current user id.
2. Lookup published lesson.
3. Validate active enrollment for lesson course.
4. Validate lesson accessibility window/release rules.
5. Lookup resources ordered by `position ASC`.
6. Return resource list without direct permanent file URLs.
### C. API tests
- returns ordered resource metadata for eligible student.
### D. DataJpa tests
- repository ordering by lesson/position.
### E. Important/Critical validations to include
- do not leak signed/direct download URLs from list endpoint.

## GET /learn/resources/{resourceId}/download-url
### A. What this API should accomplish
- Return short-lived signed download URL only when resource belongs to an accessible lesson.
### B. Steps this API should follow
1. Resolve current user id.
2. Lookup resource by `resourceId`, else 404.
3. Lookup parent lesson and enforce published.
4. Validate active enrollment for lesson course.
5. Validate lesson accessibility window/release rules.
6. Generate signed URL with object path + filename + mime type.
7. Return URL with expiry metadata.
### C. API tests
- returns signed URL payload for accessible resource.
- returns 404 for unknown resource.
### D. DataJpa tests
- resource lookup by id and lesson binding.
### E. Important/Critical validations to include
- never mint signed URL for unauthorized user or locked lesson.

