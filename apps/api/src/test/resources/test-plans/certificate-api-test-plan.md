# CertificateApi Test Plan

## POST /student/courses/{courseId}/certificate
### A. What this API should accomplish
- Issue a course certificate for an eligible student, or return existing certificate idempotently.
### B. Steps this API should follow
1. Resolve current authenticated user.
2. Validate course exists.
3. Lookup existing certificate by `(userId, courseId)` and return it if present.
4. Validate user enrollment exists for course.
5. Validate enrollment is `ACTIVE` and not expired.
6. Count published lessons in course.
7. Count user completed lessons in that course.
8. Require `totalPublishedLessons > 0` and `completed >= total`.
9. Generate unique certificate code.
10. Persist certificate with recipient/course snapshot fields.
11. Resolve instructor display name from `CourseInstructor` (PRIMARY preferred).
12. If PDF exists, mint signed download URL metadata.
13. Return issue response.
### C. API tests
- issues certificate when completion criteria met.
- idempotently returns existing certificate on repeated request.
- returns 403 when completion criteria not met.
### D. DataJpa tests
- certificate uniqueness/finders by `(user,course)` and `certificateCode`.
- published lesson count + completed progress count query usage.
### E. Important/Critical validations to include
- only active, non-expired enrollment can issue certificate.
- cannot issue when no published lessons are complete.

## GET /student/certificates/{certificateId}/download
### A. What this API should accomplish
- Return signed, temporary PDF download URL for owner’s non-revoked certificate.
### B. Steps this API should follow
1. Resolve current user id.
2. Lookup certificate by ownership.
3. Reject revoked certificates.
4. Reject certificates missing PDF URL.
5. Generate signed R2 URL with deterministic filename/content type.
6. Return download payload.
### C. API tests
- returns signed URL for owner.
- returns 403 for revoked certificate.
- contract-gap: non-owner access should return 403 by API contract.
### D. DataJpa tests
- ownership-aware finder `findByIdAndUserId`.
### E. Important/Critical validations to include
- must never mint URL for unauthorized user.

## GET /public/certificates/verify/{code}
### A. What this API should accomplish
- Publicly verify certificate authenticity and revocation status.
### B. Steps this API should follow
1. Lookup certificate by `certificateCode`.
2. Resolve instructor display name (PRIMARY preferred).
3. Compute completion metrics from published lesson count and user completion count.
4. Set status fields: `VALID` vs `REVOKED`.
5. Return public verification payload.
### C. API tests
- returns `VALID` response for active certificate.
- returns `REVOKED` response for revoked certificate.
### D. DataJpa tests
- lookup by certificate code.
- instructor relation fetch for display.
### E. Important/Critical validations to include
- never expose private-user-only fields beyond intended verification payload.

