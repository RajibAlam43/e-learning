# AdminApi Test Plan

## Course endpoints (`GET /admin/courses`, `POST /admin/courses`, `GET/PATCH /admin/courses/{courseId}`, `POST /admin/courses/{courseId}/publish|unpublish`)
### A. What this API should accomplish
- Let admins manage full course lifecycle from draft creation to publish/unpublish.
### B. Steps this API should follow
1. Authorize caller as admin.
2. For create: resolve current user from auth and create draft course with normalized title/slug.
3. For get/update: load course by id or return 404.
4. For update: patch only provided fields and parse enum strings safely.
5. For publish: set `status=PUBLISHED` and `publishedAt=now`.
6. For unpublish: set `status=DRAFT`.
7. Return admin detail/summary payload with sections and assigned instructors.
### C. API tests
- create course persists draft and creator.
- get/update returns updated values.
- publish persists status and timestamp.
- unpublish returns draft status.
### D. DataJpa tests
- `courses.slug` unique constraint holds.
### E. Important/Critical validations to include
- publish should reject incomplete course structure (contract expectation).
- invalid enum values should return 400.

## Section and lesson endpoints (`POST/PATCH/DELETE /admin/courses/{courseId}/sections`, `PATCH/DELETE /admin/sections/{sectionId}`, `POST/PATCH/DELETE /admin/sections/{sectionId}/lessons`, `POST /admin/courses/{courseId}/structure/reorder`)
### A. What this API should accomplish
- Allow admins to build and reorder course learning structure.
### B. Steps this API should follow
1. Lookup parent course/section.
2. Validate parent-child integrity (section belongs to course, lesson belongs to section for reorder).
3. Create or patch section/lesson fields (trim text fields, parse lesson/release type).
4. Delete target section/lesson safely.
5. Reorder section and lesson positions atomically for provided ids.
### C. API tests
- create section then lesson and verify returned payload.
- reorder updates persisted positions.
- delete path behavior (covered by lifecycle persistence checks).
### D. DataJpa tests
- section position unique per course.
- lesson position unique per section.
### E. Important/Critical validations to include
- invalid `lessonType` or `releaseType` should return 400.
- section/lesson 404 when ids missing.

## Media endpoints (`POST /admin/media-assets`, `PATCH /admin/media-assets/{mediaAssetId}`)
### A. What this API should accomplish
- Attach a single playable media asset to a lesson and allow admin updates.
### B. Steps this API should follow
1. Lookup lesson for create.
2. Reject when lesson already has media asset.
3. Persist provider, playback identifiers, playback mode, status.
4. Lookup media asset by id for update and patch provided fields.
### C. API tests
- create media asset persists playback data.
- update media title/provider fields persists.
- contract gap: canonical update path should be `/admin/media-assets/{id}`.
### D. DataJpa tests
- `existsByLessonId` reflects create and prevents duplicates.
### E. Important/Critical validations to include
- duplicate media on same lesson should be 400.

## Instructor endpoints (`GET/POST/PATCH /admin/instructors`, `POST /admin/courses/{courseId}/instructors`)
### A. What this API should accomplish
- Create/update instructors and assign them to courses.
### B. Steps this API should follow
1. For create: enforce unique email/phone and create `users` + `instructor_profiles`.
2. Attach `ROLE_INSTRUCTOR` mapping.
3. For update: patch user and profile fields.
4. For assignment: verify course exists, instructor exists, and user has instructor profile.
5. Insert course assignment idempotently.
### C. API tests
- create instructor returns detail payload.
- assign instructor to course persists `course_instructors` row.
### D. DataJpa tests
- instructor profile existence lookup by user id.
### E. Important/Critical validations to include
- unique contact conflicts should return 409.
- non-instructor assignment should return 400.

## Live class endpoints (`GET /admin/live-classes`, `POST /admin/courses/{courseId}/live-classes`, `PATCH /admin/live-classes/{liveClassId}`, `POST /admin/live-classes/{liveClassId}/start`)
### A. What this API should accomplish
- Schedule and start live classes linked to course/section/lesson hierarchy.
### B. Steps this API should follow
1. Lookup course, section, lesson.
2. Validate section belongs to course and lesson belongs to section.
3. Validate `endsAt > startsAt`.
4. Create live class in `SCHEDULED`.
5. Update mutable fields and optional status transitions.
6. Start endpoint sets `status=LIVE` and returns host/join metadata.
### C. API tests
- create live class with valid hierarchy succeeds.
- start live class transitions to `LIVE`.
### D. DataJpa tests
- live-class lookup and registrant count query usage.
### E. Important/Critical validations to include
- invalid time range should return 400.
- hierarchy mismatch should return 400.

## Quiz endpoints (`POST /admin/courses/{courseId}/quizzes`, `PATCH /admin/quizzes/{quizId}`, `POST /admin/quizzes/{quizId}/publish`)
### A. What this API should accomplish
- Build quiz definitions and publish only when questions are valid.
### B. Steps this API should follow
1. Lookup course and create draft quiz.
2. Insert all questions and nested choices.
3. On update with questions: replace old questions/choices.
4. On publish: ensure at least one question.
5. Ensure each question has at least one correct choice.
6. Mark quiz `PUBLISHED`.
### C. API tests
- create quiz with question+choices persists.
- publish transitions to `PUBLISHED`.
### D. DataJpa tests
- question ordering retrieval by quiz.
- choice lookup by question id.
### E. Important/Critical validations to include
- publish should fail 400 on empty question set or no correct choice.

## Order endpoints (`GET /admin/orders`, `GET /admin/orders/{orderId}`, `PATCH /admin/orders/{orderId}`)
### A. What this API should accomplish
- Give admin operational visibility and state control over orders.
### B. Steps this API should follow
1. List all orders with customer and payment summary.
2. Lookup order detail and line items by id.
3. Parse requested status safely.
4. On `PAID`, set `paidAt` if missing.
5. On `REFUNDED`, set `refundedAt` if missing.
6. Return updated detail payload.
### C. API tests
- list and detail return order and item totals.
- update status to `PAID` persists `paidAt`.
### D. DataJpa tests
- order item retrieval by order id.
### E. Important/Critical validations to include
- invalid status should return 400.
- unknown order id should return 404.
