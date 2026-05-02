# InstructorApi Test Plan

## GET /instructor/dashboard
### A. What this API should accomplish
- Return instructor dashboard with profile summary, assigned course snapshots, and upcoming live classes.
### B. Steps this API should follow
1. Resolve authenticated instructor user.
2. Load optional instructor profile by user id.
3. Load instructor course assignments.
4. Batch compute per-course active/completed enrollment counts.
5. Batch compute per-course section, lesson, and live-class counts.
6. Build course snapshot rows with role and quick links.
7. Query upcoming live classes for assigned courses (SCHEDULED/LIVE and not ended).
8. Filter upcoming list to classes owned by current instructor and cap to 10.
9. Compute approved registrant counts for upcoming classes.
10. Build dashboard totals and return response.
### C. API tests
- returns profile + course + student aggregates.
- returns upcoming live class list with registrant count.
### D. DataJpa tests
- course assignment query by instructor id.
- enrollment aggregate count queries by course ids/status.
- section/lesson/live class count queries by course ids.
### E. Important/Critical validations to include
- dashboard must not include classes owned by other instructors.

## POST /instructor/courses/{courseId}/live-classes
### A. What this API should accomplish
- Create a scheduled live class only for an instructor assigned to the target course with valid mapping/schedule.
### B. Steps this API should follow
1. Resolve authenticated instructor.
2. Load target course; reject if missing.
3. Validate instructor assignment for course.
4. Load section and lesson from request.
5. Validate section+lesson belong to course and lesson belongs to section.
6. Validate schedule (`endsAt > startsAt`).
7. Normalize title and map provider fields/fallback URLs.
8. Persist live class in `SCHEDULED` status.
9. Return detailed live class response.
### C. API tests
- creates class successfully for assigned instructor.
- rejects with 403 for non-assigned instructor.
### D. DataJpa tests
- instructor ownership existence check query.
- `findByIdAndInstructorId` retrieval support.
### E. Important/Critical validations to include
- course/section/lesson cross-mapping must be strict.

## POST /instructor/live-classes/{liveClassId}/start
### A. What this API should accomplish
- Transition an owned class to LIVE and return host/start metadata and registrant counts.
### B. Steps this API should follow
1. Resolve instructor id.
2. Load owned live class by `(liveClassId, instructorId)`.
3. Reject when status is CANCELLED/COMPLETED.
4. Set status to LIVE and persist.
5. Count approved and pending registrants.
6. Return start payload with effective host URL + meeting id.
### C. API tests
- starts scheduled class and returns LIVE status payload.
- rejects starting completed/cancelled class with 400.
### D. DataJpa tests
- registrant count queries by status.
### E. Important/Critical validations to include
- instructor ownership check is mandatory.

## PATCH /instructor/live-classes/{liveClassId}
### A. What this API should accomplish
- Update owned live class metadata/schedule/status with schedule validation.
### B. Steps this API should follow
1. Resolve instructor id.
2. Load owned live class.
3. Apply non-null updatable fields (title/description/status/times).
4. Validate effective updated schedule.
5. Persist and return updated response.
### C. API tests
- updates title/status/timing for owned class.
### D. DataJpa tests
- owner-scoped live class retrieval.
### E. Important/Critical validations to include
- reject invalid schedule combinations.

## DELETE /instructor/live-classes/{liveClassId}
### A. What this API should accomplish
- Soft-delete (cancel) owned class while preserving history.
### B. Steps this API should follow
1. Resolve instructor id.
2. Load owned live class.
3. Reject deletion when class is LIVE or COMPLETED.
4. Set status to CANCELLED and persist.
### C. API tests
- cancels deletable classes.
- rejects deletion of LIVE/COMPLETED classes with 403.
### D. DataJpa tests
- owner-scoped live class retrieval and persisted status mutation.
### E. Important/Critical validations to include
- never hard-delete class records.

## Contract Gap to Track
### A. Expected behavior
- Invalid create payloads with blank title should return 400 via bean validation.
### B. Current risk
- `CreateLiveClassRequest` constraints may not trigger if `@Valid` is missing on controller method request body.

