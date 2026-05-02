# StudentApi Test Plan

## GET /student/dashboard
### A. What this API should accomplish
- Return student dashboard aggregate with enrolled courses, ongoing progress, certificates, and upcoming classes.
### B. Steps this API should follow
1. Resolve current authenticated user.
2. Load user profile (optional).
3. Load enrolled course summaries.
4. Load certificates.
5. Load upcoming live classes.
6. Compute ongoing/completed counts and slice top items.
7. Return dashboard DTO.
### C. API tests
- returns dashboard with correct total counts.
- limits ongoing/recent/upcoming lists to max 5.
### D. DataJpa tests
- enrollment + lesson progress aggregate queries.
### E. Important/Critical validations to include
- must never include other users' data.

## GET /student/courses
### A. What this API should accomplish
- Return active enrolled courses only with completion and certificate flags.
### B. Steps this API should follow
1. Resolve user.
2. Load ACTIVE enrollments only.
3. Batch load lessons/completions/certificates/instructors.
4. Map per-course completion metrics.
### C. API tests
- includes only active enrollments.
- computes completion percentage from published lessons.
### D. DataJpa tests
- lesson progress completion count query by user/course.
### E. Important/Critical validations to include
- completion denominator must be published lessons.

## GET /student/courses/{courseId}
### A. What this API should accomplish
- Return detailed enrolled course home view with sections/lessons/progress.
### B. Steps this API should follow
1. Resolve user and ACTIVE enrollment for course.
2. If not found -> 404.
3. Load published sections and lessons.
4. Load user lesson progress for course.
5. Build section/lesson home responses and completion stats.
6. Attach active certificate info.
### C. API tests
- returns details for active enrollment.
- returns 404 for not-enrolled course.
### D. DataJpa tests
- section + lesson published ordering queries.
### E. Important/Critical validations to include
- enforce enrollment ownership.

## GET /student/orders
### A. What this API should accomplish
- Return user order history with item breakdown and final amounts.
### B. Steps this API should follow
1. Resolve user.
2. Load user orders sorted desc.
3. Load order items per order.
4. Map item totals and order metadata.
### C. API tests
- returns only current user orders.
### D. DataJpa tests
- order lookup by user and item lookup by order.
### E. Important/Critical validations to include
- no cross-user order leakage.

## GET /student/certificates
### A. What this API should accomplish
- Return user certificates with revoked flag and verification link.
### B. Steps this API should follow
1. Resolve user.
2. Load certificates for user ordered by issued date.
3. Map verification URL + revocation flags.
### C. API tests
- includes revoked and non-revoked with correct flags.
### D. DataJpa tests
- certificate list ordering by issuedAt desc.
### E. Important/Critical validations to include
- certificate ownership isolation.

## GET /student/live-classes
### A. What this API should accomplish
- Return upcoming classes for active enrollments with join eligibility.
### B. Steps this API should follow
1. Resolve user and active enrollments.
2. Load upcoming live classes by statuses/time.
3. Load registrant rows for user/class ids.
4. Derive `isLive`, `isRegistered`, `canJoin`, and effective join URL.
### C. API tests
- returns upcoming classes only.
- exposes joinUrl only when canJoin=true.
### D. DataJpa tests
- upcoming live class query by courseIds + statuses + now.
### E. Important/Critical validations to include
- no class exposure outside enrolled courses.

## GET /student/courses/{courseId}/live-classes
### A. What this API should accomplish
- Return course-specific classes only for active enrollment.
### B. Steps this API should follow
1. Resolve user + active enrollment for course.
2. If not found -> 404.
3. Load all course live classes ordered by startsAt.
4. Load registrant info and map summary.
### C. API tests
- returns 404 for non-enrolled course.
- returns mapped summaries for enrolled course.
### D. DataJpa tests
- course live-class query ordering.
### E. Important/Critical validations to include
- strict enrollment gate.

## POST /student/live-classes/{liveClassId}/join
### A. What this API should accomplish
- Validate enrollment/registration/time gate and return join payload.
### B. Steps this API should follow
1. Resolve user.
2. Load live class or 404.
3. Validate active enrollment for class course.
4. Validate enrollment not expired.
5. Validate approved registrant.
6. Enforce join window (allow live, completed, or shortly-before-start).
7. Resolve effective join URL else 403.
8. Return join response.
### C. API tests
- returns join info for eligible student.
- returns 404 for unknown class.
- returns 403 for non-enrolled / not-registered / too-early.
### D. DataJpa tests
- registrant lookup by class+user+status.
### E. Important/Critical validations to include
- access must be denied unless all gates pass.
