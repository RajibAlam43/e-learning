# Semesters and academic credits

Steps 1 and 2 provide institution semesters, course credits, reusable cohort offerings,
completion-based achievements, enforced prerequisites and credit-bearing waivers.
Program credit requirements and manual enrollment belong to later steps.

## Administration

Use **Admin → Semesters** to create or edit a semester. Supply a unique code, name,
teaching dates and registration dates. The screen uses Asia/Dhaka wall-clock time;
the API stores instants. Registration may overlap teaching but must close by the end
of the semester. Status is derived from dates rather than requiring a manual switch.

New cohort-based courses require a semester. Teaching and registration dates default
to that semester; narrower offering windows are allowed. Moving a semester cannot
invalidate an attached offering. Registration checks enforce the semester window for
both free enrollment and checkout; payment activation retains purchase-time eligibility.
Self-paced offerings can remain unassigned.

Existing cohorts are not automatically assigned to an invented semester. They remain
usable; administrators can assign them through the course editor. Existing course
credits default to 3, but old completions receive no automatic credit backfill.

Use a course's **Cohorts** tab to create another batch that shares its course template,
lessons, quizzes and credit identity. Teachers, enrollments, class meetings and capacity
remain offering-specific. Curriculum changes affect all offerings sharing that template.
The existing `/repeat` API still creates an independent curriculum copy and therefore
an independent academic course identity; use `/offerings` for another cohort.

## Credits

Credits are positive whole numbers, defaulting to 3, on the reusable course template.
The course create/edit forms expose this value. Course detail responses include the
course-template ID, credits and semester; public course responses include credits and
semester information as well.

A new successful course completion awards an academic achievement in the same database
transaction that records completion. The certificate issuance fallback uses the same
award path when it is the first operation to record completion. A unique
`(user_id, course_template_id)` constraint and `INSERT ... ON CONFLICT DO NOTHING` make
retries and concurrent completions of different cohorts idempotent.

Awards snapshot the title, credits, semester and completion time. Later edits to a
course's credits or completion of another cohort cannot change or duplicate an existing
award. Expiry of learning access does not by itself erase the academic record. No
historical completion backfill or exemption award endpoint is introduced by this step.

## API

All administration routes require ADMIN or SUPER_ADMIN. Academic-record routes expose
only the authenticated student's own record, or a student explicitly requested by an
administrator.

| Method | Route | Purpose |
| --- | --- | --- |
| GET / POST | `/admin/semesters` | List / create semesters |
| PUT | `/admin/semesters/{semesterId}` | Update semester details and dates |
| GET / POST | `/admin/courses/{courseId}/offerings` | List / create offerings sharing the curriculum |
| GET | `/admin/students/{userId}/academic-record` | Read a student's awarded credits |
| GET | `/student/academic-record` | Read your own awarded credits |

Course create/repeat requests accept `semesterId`; create/update requests accept
`credits`. Missing credits on creation use 3. Patching an existing course without a
semester field preserves its assignment. Assigned cohort-based offerings cannot have
their semester silently removed. The standard course endpoints remain under their
existing admin permissions.

## Migration and release

Apply **V24** through the existing migrator release process, then deploy the compatible
API/worker and frontend images. The migration adds tables and columns and preserves
existing completion and certificate records. It does not configure a production
semester, grant historical credits or deploy infrastructure.

Backend integration tests exercise migration/Hibernate validation, semester bounds,
shared offerings, credit snapshots, concurrent awards and record authorization.
The generated frontend API types come from the tested OpenAPI document at
`apps/api/build/openapi/academic-step-one.json`.

## Prerequisites and credit waivers (step 2)

**Admin → Courses → Overview → Required courses** defines enforced prerequisite
course templates. Rules are shared by all cohorts and are separate from the existing
free-text prerequisite descriptions. Self-links, duplicates and indirect cycles are
rejected; concurrent graph edits are serialized in PostgreSQL.

Standalone checkout, free enrollment, collection checkout and payment fulfillment use
the same prerequisite policy. A completed enrollment in any cohort or an academic
credit award for the required template satisfies the rule. Historical completions
qualify without automatically backfilling their academic credits. Merely owning or
buying a prerequisite does not satisfy it, including buying both courses together.
Existing active enrollments retain access. New purchases capture prerequisite
eligibility per offering; edits after checkout do not invalidate that decision when
payment is initiated or fulfilled. Existing pending/paid orders are grandfathered by
V25 because they predate these rules. Semester/window and capacity checks still apply.

**Student → Credit Waivers** lets a student request full course credit for prior
learning, explain the request and optionally attach one PDF, JPEG or PNG up to 10 MB.
The course must be published or required by a published course. Only one pending
request per student/template is allowed; rejected requests may be submitted again.
Students can see only their own requests and supporting files.

**Admin → Credit Waivers** lists pending, approved and rejected requests, with paging.
Approval/rejection requires a reason and records the reviewer and decision time.
Approval grants the course's current credits and snapshots them on an `EXEMPTION`
achievement. If credits changed while the review screen was open, refresh/reselect
the request and review the new value before approving. An optional semester attributes
the academic award; it does not enroll the student into a cohort. Repeating the same
review decision is idempotent; changing a final decision is rejected.

The unique student/template award constraint also covers simultaneous course
completion and waiver approval. An existing award is preserved, including its original
credits and source. Exemptions satisfy subsequent prerequisites and mandatory collection
requirements across cohorts, without changing lesson progress, exams, attendance or
issuing a course-completion certificate. Program credit/elective graduation rules remain
part of the later program steps.

Supporting files use the existing **private** R2 bucket and credentials, never the
public thumbnail bucket. The API checks ownership, object existence, size and MIME
metadata before attaching a file. Authorized downloads use signed attachment URLs.
Private-bucket CORS must allow the web origin to PUT uploads with Content-Type, as for
existing private uploads. No new environment variables or public bucket are needed.
Abandoned uploads can remain in storage; no automatic orphan cleanup is introduced.

| Method | Route | Purpose |
| --- | --- | --- |
| GET / PUT | `/admin/courses/{courseId}/prerequisites` | Read / replace enforced prerequisites |
| GET | `/student/courses/{courseId}/eligibility` | List the student's missing prerequisites |
| GET | `/student/waivers/courses` | List eligible waiver course templates |
| GET / POST | `/student/waivers` | Own requests / submit a request |
| POST | `/student/waivers/evidence-upload` | Prepare a private supporting-file upload |
| GET | `/student/waivers/{id}/evidence` | Download your own supporting file |
| GET | `/admin/waivers` | Review queue; `status` and zero-based `page` filters |
| POST | `/admin/waivers/{id}/review` | Approve or reject with a review reason |
| GET | `/admin/waivers/{id}/evidence` | Administrator supporting-file download |

Request lists have 50 entries per page. Apply **V25** after V24 through the migrator,
then deploy the compatible API, worker and frontend together. No production data or
infrastructure is changed by local implementation or tests.
