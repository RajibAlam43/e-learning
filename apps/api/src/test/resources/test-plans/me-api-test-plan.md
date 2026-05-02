# MeApi Test Plan

## GET /me
### A. What this API should accomplish
- Return authenticated user identity, profile, optional instructor profile, and user-level aggregate stats.
### B. Steps this API should follow
1. Resolve current authenticated user.
2. Load `UserProfile` by user id (optional).
3. Load `InstructorProfile` by user id (optional).
4. Count active enrollments.
5. Count completed active enrollments.
6. Count active (non-revoked) certificates.
7. Count attended live classes.
8. Build `MeResponse` with defaults (`locale` fallback, empty permissions list).
### C. API tests
- returns user base fields + profile + instructor profile.
- returns aggregate counts from enrollment/certificate/attendance data.
### D. DataJpa tests
- user lookup by email/phone and existence checks.
- aggregate count queries used by profile response.
### E. Important/Critical validations to include
- never return another user’s profile/stats.

## PATCH /me/profile
### A. What this API should accomplish
- Update user core profile fields, normalize input, resolve uniqueness conflicts, and upsert user/instructor profile records.
### B. Steps this API should follow
1. Resolve current authenticated user.
2. Load existing `UserProfile` (optional).
3. Apply user updates:
4. trim full name when provided.
5. normalize email lowercase + uniqueness conflict check.
6. normalize phone + uniqueness conflict check.
7. normalize `phoneCountryCode` to `+digits` format.
8. save updated user entity.
9. Upsert `UserProfile` with avatar/locale/timezone/bio and locale default.
10. If instructor payload exists, upsert `InstructorProfile`.
11. Recompute aggregates and return full `MeResponse`.
### C. API tests
- updates and normalizes fields; returns updated profile.
- returns `409` when email/phone conflicts with another user.
### D. DataJpa tests
- profile/instructor-profile persistence and retrieval by user id.
- uniqueness guard lookups (`existsByEmail`, `existsByPhone`).
### E. Important/Critical validations to include
- changing email/phone resets verification timestamps.
- conflict checks must exclude self-value updates.

## Contract Gap to Track
### A. Expected behavior
- Invalid `phoneCountryCode` input should return `400`.
### B. Current gap
- current normalization strips non-digits and can silently convert invalid input to `null` instead of rejecting.

