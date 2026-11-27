# Academic plan acceptance validation

Validated against the supplied seven-step development plan on November 26, 2026.
This records repository behavior and local verification. It does not assert that a
hosted environment has been migrated or deployed.

## Results against the supplied plan

| Plan area | Implemented behavior | Verification |
| --- | --- | --- |
| 1. Semesters and credits | Institution semesters and registration dates; offering-specific cohorts and teachers; independent self-paced offerings; whole-number template credits defaulting to 3; immutable completion/exemption awards once per student/template. | `AcademicFoundationApiIt`, including simultaneous repeated-cohort completion, plus the semester/cohort browser flow. |
| 2. Prerequisites and waivers | Structured links reject duplicates, self-links and cycles. Shared eligibility gates purchases, free enrollment, payment fulfillment, program registration and manual admission. Approved, audited waivers award credits once without fabricating course activity. Pending paid orders retain their eligibility decision. | `PrerequisitesAndWaiversApiIt`, including authorization, private evidence, changed-credit review and concurrent approval/completion; student/admin waiver browser flow. |
| 3. Program controls | Manage program supports ordered approved courses, mandatory/elective classification, required credits, elective allowance and access durations. Unpublished courses appear as Coming soon with learning content unavailable. Validation requires an achievable combination of whole elective courses within the cap. | `ProgramCurriculumApiIt` and the program curriculum browser flow. Controls live in the existing Manage program page rather than a separate modal. |
| 4. Purchase and registration | Credit-based program purchases save their contract and grant entitlement without taking seats. Later registrations enforce access, approved curriculum, prerequisites, dates, capacity and remaining allowance. Unfinished choices can be withdrawn with an audit reason. | `ProgramCourseRegistrationApiIt`: last-seat and allowance races, duplicates, expiry, prerequisites, withdrawals and refunds; registration browser flow. |
| 5. Later additions | Existing purchasers receive later choices within their original period and allowance. Original mandatory requirements, credit target, saved course credit costs and expiry remain fixed. Original purchased courses and registered later additions survive catalog removal. | `ProgramCurriculumApiIt` and `ProgramCourseRegistrationApiIt`, including saved credit costs across preview, checkout and registration. |
| 6. Manual and student controls | Admins select an existing student and offering, supply a reason and optionally choose an expiry. Date/capacity exceptions are explicit and audited; prerequisites require an approved waiver. Student components expose blockers, cohorts/semesters, access expiry, mandatory progress and elective usage. | `ManualEnrollmentApiIt`, including expiry/retry behavior and access preservation; manual enrollment, curriculum and graduation browser flows. |
| 7. Graduation, certificates and migration | All original mandatory courses and the required eligible credits must be satisfied. Progress and program certificates share the same requirements calculation. Credits are reusable across eligible programs and count once within each. Actual course completion criteria remain in use. Historical records survive the additive upgrade. | `GraduationApiIt`, existing certificate suites and `AcademicMigrationCompatibilityIt`; student/admin graduation browser flow. |

Previously completed or exempted eligible courses are recognized automatically in
program credit progress and allowance. They require no repeat cohort, seat or payment;
the system does not create a second registration merely to recognize an existing award.
An academic waiver never creates a course-completion certificate without actual course
completion. Legacy purchases retain their original bundle rules; deferred registration
and the credit requirements apply to programs configured with academic requirements.

## Gaps corrected during final validation

- Added the optional manual course expiry, with future-date validation, request auditing
  and retry matching. Existing valid access is preserved rather than shortened.
- Program validation now checks whole-course combinations. For example, a 3-credit core
  plus 3-credit electives cannot reach 7 credits inside a 4-credit elective allowance.
- Program previews, registration costs and protection against duplicate standalone
  charges use the purchaser's saved course credits even after template edits.
- Course certificate completion fallback now refreshes program graduation after awarding
  credits. Both course and program certificate backfill take an enrollment lock and
  enforce active, unexpired access. Existing certificates remain available unless revoked.
- Planned catalog entries use the requested Coming soon wording.

## Verification evidence

- **499 backend tests passed:** 134 API unit tests, 348 integration tests, 17 worker tests;
  no failures or skipped tests. Java formatting, style checks and backend packaging passed.
- Frontend route type generation, generated API types, TypeScript checking, linting and
  the production webpack build passed.
- Six API-mocked browser flows passed against that build: semester/cohort administration,
  prerequisites/waivers, program curriculum, program registration/withdrawal, manual
  enrollment with custom expiry, and graduation/certificates. Each checked desktop
  (1280 px) and mobile (390 px) layouts; no horizontal overflow or JavaScript page errors.
- Integration tests used the disposable native PostgreSQL 17 test override. CI's existing
  default remains Testcontainers PostgreSQL 18; no hosted database configuration changed.

## Migration and historical-credit review

`AcademicMigrationCompatibilityIt` creates a separate disposable database, migrates it
through **V23**, inserts historical purchases, repeated completed cohorts, a manual program
enrollment and existing course/program certificates, then runs the real upgrade through
**V29**. It compares all original columns in the historical tables before and after.

The rehearsal verified preserved order/payment data, course/program enrollment status,
completion and expiry dates, certificate codes and PDF object keys. Purchased course
snapshots take precedence over later catalog edits; manual legacy enrollments receive the
documented current-catalog fallback. No historical credit target or access expiry is
invented. Purchased contracts remain immutable, repeated migration is a no-op, and neither
the migration nor the review report awards historical credits or creates cohort selections.

The read-only [review report](../deploy/db/academic-migration-review.sql) lists one historical
credit candidate per student/template, its earliest completed enrollment, the number of
completed cohorts, current credit value and migration readiness counts. The current credit
value is explicitly a review input, not proof of the credit value when the student completed.
The test verifies that the report runs in a read-only transaction and changes no records.

Before production promotion:

1. Restore a backup into an isolated review/staging database, apply the additive migrations
   through V29 using the existing migrator, and rehearse restoration as in the deployment runbook.
2. Review existing credit-based program configurations using the updated whole-course
   allowance validation, and review old cohorts that still need semester assignments.
3. Run the report using an operator-configured libpq service for that restored database:

   ```sh
   psql 'service=gii_academic_review' -X -q -A -t -v ON_ERROR_STOP=1 \
     -f deploy/db/academic-migration-review.sql > academic-migration-review.json
   ```

   The service name is an example; configure the actual connection using the runbook's
   verified TLS and environment separation. Keep report output with private migration records.
4. Investigate any missing purchase snapshots or missing purchased-course entitlements.
   Review candidate history and credit values before deciding any completion-to-credit backfill.
5. Promote the matching migration/API/worker/web release through the existing process.
   Production deployment remains manual.

**V29 is required** for the optional manual expiry audit field. V24–V28 are unchanged.
No new environment variables or services are required. No production data review, historical
credit backfill, hosted migration or deployment was performed during this implementation.
