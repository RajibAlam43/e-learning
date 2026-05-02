# AuthApi Test Plan

## POST /public/auth/register
### A. What this API should accomplish
- Register a user using exactly one identifier (email or phone), assign `STUDENT` role, and issue verification flow.
### B. Steps this API should follow
1. Validate request body.
2. Normalize email/phone.
3. Enforce identifier rules (one required, both not allowed).
4. Enforce uniqueness.
5. Enforce phone country code when phone registration is used.
6. Persist user and role mapping.
7. Generate verification code for the chosen channel.
8. Return user id + verification channel.
### C. API tests
- successful email registration creates user + role + verification code.
- successful phone registration creates user + role + verification code.
- contract gap: both email and phone should return `400`.
- contract gap: duplicate identifier should return `409`.
### D. DataJpa tests
- user repository uniqueness/lookup behavior for email + phone.
- user-role persistence for new user.
### E. Important/Critical validations to include
- exactly one identifier rule.
- no partial persistence (user without role/code) on failure.

## POST /public/auth/login
### A. What this API should accomplish
- Authenticate credentials; if verified, issue access token + refresh cookie; if unverified, trigger verification flow.
### B. Steps this API should follow
1. Normalize identifier by channel.
2. Resolve user by channel.
3. Ensure active user.
4. Verify password hash.
5. If verified channel: create access + refresh token, set cookie, return identity payload.
6. If unverified channel: issue verification code and return `isVerified=false`.
### C. API tests
- verified email login returns JWT + refresh cookie.
- unverified email login returns `isVerified=false` and creates OTP.
- contract gap: invalid credentials should return `401` (currently runtime error path).
### D. DataJpa tests
- verification code latest-active query behavior.
- refresh-token persistence lifecycle.
### E. Important/Critical validations to include
- invalid credentials must not reveal account existence.
- verified login must always rotate new refresh token session artifact.

## POST /public/auth/refresh
### A. What this API should accomplish
- Rotate refresh token and issue a new access token.
### B. Steps this API should follow
1. Read refresh token from cookie.
2. Validate token exists, active, and not expired/reused.
3. Revoke/mark prior token and persist replacement.
4. Issue new access token.
5. Set replacement refresh cookie.
### C. API tests
- valid refresh rotates token and sets new cookie.
- missing cookie returns `400`.
- contract gap: invalid/reused token should map to `401/403`.
### D. DataJpa tests
- session token retrieval by session id + revoked filter.
- active token query by user id.
### E. Important/Critical validations to include
- reuse detection revokes entire active session.

## POST /public/auth/forgot-password
### A. What this API should accomplish
- Trigger password reset OTP flow without account enumeration.
### B. Steps this API should follow
1. Normalize identifier.
2. Lookup user by channel.
3. If user missing, still return success response.
4. If user exists, create password-reset OTP.
### C. API tests
- unknown user still returns `200`.
- existing user returns `200` and creates reset OTP.
### D. DataJpa tests
- verification code counts and latest query for password reset.
### E. Important/Critical validations to include
- response parity between existing/non-existing account.

## POST /public/auth/reset-password
### A. What this API should accomplish
- Verify reset OTP, update password hash, revoke active refresh sessions.
### B. Steps this API should follow
1. Normalize identifier and resolve user.
2. Verify OTP against purpose/channel.
3. Update password hash.
4. Revoke all active refresh tokens.
5. Return success response.
### C. API tests
- valid OTP updates password and revokes active refresh tokens.
- contract gap: invalid/expired OTP should return `400`.
### D. DataJpa tests
- refresh token revoked filtering.
### E. Important/Critical validations to include
- all active refresh tokens must be revoked after password reset.

## POST /public/auth/send-code
### A. What this API should accomplish
- Send verification code based on channel + purpose safely.
### B. Steps this API should follow
1. Normalize identifier and resolve user.
2. Keep enumeration-safe success when user not found.
3. Validate purpose/channel compatibility.
4. Skip already-verified channel.
5. Enforce cooldown/rate-limit checks.
6. Persist new OTP record.
### C. API tests
- existing unverified email generates code.
- unknown identifier returns `200`.
- contract gap: cooldown/rate-limit should map to `429`.
- contract gap: invalid purpose/channel pair should map to `400`.
### D. DataJpa tests
- recent OTP count queries for user/channel + channelHash.
### E. Important/Critical validations to include
- per-user and per-identifier cooldown both enforced.

## POST /public/auth/verify-code
### A. What this API should accomplish
- Verify OTP then mark user channel as verified.
### B. Steps this API should follow
1. Normalize identifier and resolve user.
2. Verify OTP (purpose/channel/user-bound).
3. Mark corresponding verification timestamp (`emailVerifiedAt` or `phoneVerifiedAt`).
4. Persist user update.
### C. API tests
- valid email verification marks `emailVerifiedAt`.
- contract gap: invalid/expired OTP should return `400`.
### D. DataJpa tests
- latest valid OTP lock/query semantics.
### E. Important/Critical validations to include
- wrong purpose/channel must not verify account.

## POST /public/auth/logout
### A. What this API should accomplish
- Revoke active session refresh tokens and clear cookie.
### B. Steps this API should follow
1. Read refresh token cookie (optional).
2. If token exists: resolve token hash and session id.
3. Revoke all non-revoked tokens in that session.
4. Clear refresh cookie on response.
### C. API tests
- logout with refresh token revokes session tokens and clears cookie.
- logout without token still clears cookie and returns success.
### D. DataJpa tests
- find-by-session-id and revoke persistence behavior.
### E. Important/Critical validations to include
- cookie must always be cleared even when token missing/unknown.
