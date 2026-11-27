# Semesters and academic credits

Steps 1–6 provide institution semesters, course credits, reusable cohort offerings,
academic achievements, prerequisites, credit waivers, program curricula and deferred
course/cohort registration, audited manual enrollment administration, and graduation reporting.

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

Standalone checkout, free enrollment, legacy bundle checkout and course payment
fulfillment use the same prerequisite policy. A completed enrollment in any cohort or an academic
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
issuing a course-completion certificate. Credit-based program rules are added in step 3
below; expanded graduation administration belongs to the later graduation step.

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


## Program curricula and purchase contracts (step 3)

Open **Admin → Programs → Manage program** to edit program details and curriculum.
Select published or planned/unpublished courses, reorder them, and mark each mandatory
or elective. Multiple cohorts of the same template are one academic course and cannot
be listed twice in a credit-based curriculum. Planned courses expose their title and
credits in the program syllabus, but their individual course page and study content
remain unpublished.

Saving credit requirements opts the program into credit-based purchases. Legacy
collections retain their existing bundle behavior until configured; legacy publishing
still requires published courses. Credit-based programs can remain published while
courses and requirements are edited. Publication and checkout reject graduation
requirements that cannot be reached: the total must include all mandatory course
credits and fit within the offered electives and configured elective credit allowance.
New purchases capture their own rules, so changing these settings affects future buyers.

Set expected duration in months. Access defaults to that duration **plus 24 months**;
an explicit access duration overrides it. A confirmed program payment creates program
access ending that many calendar months after the payment confirmation time, calculated
in UTC. A delayed payment worker or duplicate callback cannot extend this period.
Legacy enrollments retain their existing end dates, including any lifetime access;
no retrospective expiry or credit target is invented.

The original course requirements, mandatory flags, total credits, elective allowance
and access duration are copied to the order at checkout and to the enrollment at
payment. They are immutable in PostgreSQL. Migration V26 freezes historical bundle
requirements using the purchased course snapshot where available, with current
membership as the legacy fallback. Adding/removing/reclassifying courses later does
not rewrite an existing student's original mandatory requirements or graduation target.

Purchased students see the union of their original curriculum and later program
additions. New additions are optional for existing students. Their credits can count
within that student's original elective allowance. Removed original courses remain
part of the purchased contract. Only achievements belonging to eligible templates
count, once per template within a program. Approved waiver credits count, and the same
achievement can count toward multiple eligible programs. Elective credits are capped;
unrelated courses cannot replace a missing mandatory requirement. Legacy course and
program completion records remain intact.

Credit-based program purchase grants **program entitlement**, not automatic enrollment
into every included course/cohort and not a cohort seat reservation. It is independent
of individual course prerequisites, registration dates and seat capacity. The separate
course/cohort registration flow, including elective selection and reservation of allowance,
is implemented in step 4 below. Course-level purchase/free-enrollment checks remain enforced. The student program page shows original requirements, credit progress,
access expiry and the current/previous course catalog; it offers study links only for
actual course enrollments. Students now use the offering choices described below to
turn program entitlement into individual course access.

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/admin/collections/{id}` | Details, current courses and program requirements |
| PUT | `/admin/collections/{id}/curriculum` | Atomically save requirements and ordered courses |
| GET | `/student/collections/{id}/curriculum` | Own original requirements, credit progress and available courses |

The existing program create/edit/publish routes remain available. Public collection
responses add program requirements and published/planned syllabus metadata. Apply
**V26** after V25 through the migrator, then deploy the matching API, worker and web
images together. No new environment variables are needed. Local tests and implementation
do not migrate or deploy a hosted environment.


## Deferred course and cohort registration (step 4)

Open **My Programs → a program** to choose an offering for each included course.
The list includes published cohorts of the same academic template, including cohorts
created after the program was purchased. Planned courses remain visible until an
offering is published. Each offering shows semester, study dates, credits, access
end date, seat availability and any registration blocker. Self-paced offerings use
the same registration route.

Registration requires an active, unexpired program entitlement and a published,
included course template. New course enrollments enforce prerequisites/approved
waivers, semester and offering registration windows, and capacity. A program purchase
itself still grants no seats. The registration transaction locks the offering and
serializes the student's enrollment changes, so simultaneous requests cannot take
the last seat twice or spend the same elective allowance. Retrying the same selection
returns the original record and does not renew access or reserve another seat.

There is one active selection per student/program/course template, and a student
cannot select another simultaneous unfinished cohort of that template. An existing
valid enrollment can be linked through **Use program access** without taking another
seat or repeating admission checks. Independent course access is preserved, and
valid grants from another program are combined. Registration grants expire at the
earlier of the course access duration and the program access end. Refunds and
withdrawals recalculate access using the remaining valid sources, so they neither
remove separately funded access nor leave a refunded program's access active.

For credit-based programs, optional course selections reserve their credit value at
registration. Reused eligible elective achievements also occupy allowance; a selected
course and its achievement count once. Extra academic credits are capped at the
program's original elective allowance. Mandatory classifications and limits come
from the purchaser's original contract, not later edits. Existing legacy purchasers
keep their uncapped bundle entitlement. Selected later additions remain available
if they are subsequently removed from the current program catalog.

Courses with awarded academic credit require no new program registration. Existing
valid course access remains usable. Completing a different cohort cannot duplicate
that academic credit. Programs direct students away from separately purchasing a
course they can already register through their entitlement, including in a mixed
program/course cart. An extra elective beyond the available program allowance can
be bought separately through ordinary checkout, with its usual prerequisite, date
and capacity checks; it does not increase the graduation credit allowance.

**Admin → Students → click a student's name** opens the paged registration history.
Administrators can withdraw an unfinished selection with a required reason. The
reviewer, timestamp and reason are retained; new choices create separate history
records. Withdrawal releases an unfinished elective reservation and any access
provided solely by that choice. Completed courses or awarded academic credits cannot
be withdrawn to release allowance. Students cannot withdraw their own choices.
Date/capacity overrides and general manual enrollment are described in step 5 below.

| Method | Route | Purpose |
| --- | --- | --- |
| POST | `/student/collections/{id}/registrations` | Register for an included `courseId` offering |
| GET | `/student/collections/{id}/curriculum` | Offerings, blockers, active selection and remaining allowance |
| GET | `/admin/students/{userId}/program-registrations?page=0` | Registration history, 50 entries per page |
| POST | `/admin/students/{userId}/program-registrations/{id}/withdraw` | Withdraw an unfinished selection with `reason` |

Apply **V27** after V26 through the existing migrator. It adds registration records
and access-source bookkeeping; it does not change existing enrollments or invent
historical selections. Deploy matching API, worker and web images after migration.
No new environment variables, cloud services or hosted-environment changes are needed
for this implementation. Verification includes real PostgreSQL concurrency/access tests
and mocked-API browser flows on desktop and mobile.

The final complete integration run passed on a disposable native PostgreSQL 17 database
using the existing loopback test override after Docker's PostgreSQL 18 container failed
to initialize with a local disk I/O error. Earlier registration/concurrency checks passed
on PostgreSQL 18. No test configuration or hosted database setting was changed.

## Manual enrollment administration (step 5)

Open **Admin → Students → a student's name → Manual enrollment**. The new screen
shows the student's name and email or phone, and offers three actions:

- **Course access** grants an independent enrollment in a selected course offering.
- **Program access** grants program entitlement with its curriculum snapshot and
  configured access period. It does not take course seats; courses are selected later.
- **Course through a program** selects an included, published offering against that
  student's active program entitlement and elective allowance.

Every action requires a reason. Administrators can explicitly override semester and
course registration dates or course capacity for course admissions. Independent
course/program grants also offer an explicit unpublished-access exception. Program-funded
registrations still require a published, included offering and sufficient elective
allowance. Prerequisites are enforced for new admissions even with overrides; approve
the student's credit waiver through the existing review process first.

Existing valid enrollments are returned unchanged, with an audit entry explaining
that existing access was preserved. This action does not add a separate grant to an
already active enrollment or extend its expiry. New/reactivated independent course
access uses the offering's configured duration; absence of a duration means no expiry,
as with ordinary course enrollment. Program access uses its configured duration, or
expected duration plus 24 months for a credit-based program. Legacy bundles keep their
existing unlimited-duration semantics. Reactivating a program retains its original
graduation requirements and access-period configuration; later curriculum changes
do not rewrite that academic contract. Completed work and academic achievements remain
intact. Granting access alone creates no order, payment, completion or academic credit.

Each successful action records the student, administrator, timestamp, target, reason,
requested overrides, resulting expiry and whether access changed. The screen pages
through 50 audit entries at a time. Clients send a UUID `requestId`; replaying the same
action returns its original result without renewing access or adding an audit entry.
Reusing that ID with different content is rejected. Failed admissions create no audit
grant. Course locks serialize admissions against seat capacity, while the shared student
mutation lock coordinates these actions with purchases, refunds and program choices.

Manual independent access remains valid after withdrawal of a linked program selection.
A pending payment settling after a valid independent manual/free grant cannot replace
that grant's provenance, so refunding that payment cannot revoke the manual/free access.
Program-funded choices use the existing audited withdrawal flow and preserve other
valid access sources. Access grants do not bypass section publication or release rules.

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/admin/students/{userId}/manual-enrollments?page=0` | Student identity, active program entitlements and manual audit history |
| POST | `/admin/students/{userId}/manual-enrollments` | Grant or register access with an audit reason and explicit overrides |

POST accepts `requestId`, `reason`, and either `courseId` or `programId`. For a
program-funded course selection, provide both IDs and `programFunded: true`.
`overrideDates`, `overrideCapacity` and `allowUnpublished` default to false. Date and
capacity overrides apply only to course admissions; `allowUnpublished` applies only
to independent course/program access. Both routes require ADMIN or SUPER_ADMIN.

Apply **V28** after V27 using the existing migrator, then deploy matching API, worker
and web images. It adds only the manual enrollment audit table and index. No new
environment variables or cloud services are required. Existing frontend source changes
are limited to the registration page's navigation link and regenerated API types;
the screen, component and client hooks are new files. Nothing has been deployed.

## Academic records and graduation (step 6)

Students can open **My Programs → Academic record and graduation progress**, or the
academic-record link inside an individual program. Administrators open **Students →
a student's name → Academic record**. Both views show the student's awarded credits,
completion versus approved-waiver source, semester when assigned, and program progress.
The transcript counts each course template once, even after multiple completed cohorts.
It includes earned credits outside a program's syllabus; individual program reports count
only their eligible courses. The same eligible academic award may satisfy several programs.

Each credit-based program shows its original mandatory courses and credit target,
credits still needed, and a per-course breakdown of awarded and counted credits. Mandatory
requirements remain fixed by the original enrollment snapshot. Current additions and retained
active selections are optional for existing students. Eligible elective awards are allocated
to the original elective cap in achievement-time order, with template ID breaking ties.
A partially counted award or excess elective credits stay on the academic record. Waivers
satisfy mandatory academic requirements and contribute their awarded credits without faking
lessons, exams, attendance or a course-completion certificate.

**Check graduation** evaluates the existing rules and records `completedAt` when eligible.
Students and administrators use the same check; it cannot waive requirements or add credits.
New completion requires active, unexpired program access backed by a paid order or an
independent admin grant. Award/completion events still refresh program completion automatically.
Reading progress does not mutate enrollment. Checks are idempotent, and already recorded
completion remains intact after later catalog edits, refunds or access expiry. Reports expose
current credit allocation separately from that permanent completion timestamp. Legacy programs
continue to use their original mandatory-course requirements rather than a new credit target.

Program percentage now considers both counted credits and mandatory courses, so enough
elective credits alone cannot display 100 percent when mandatory courses are missing.
Recorded completion continues to display 100 percent. The new reports distinguish requirements
remaining, requirements met, recorded completion and inactive access.

The program certificate action follows these eligibility results and uses the existing
certificate issuance, private storage, download and public-verification routes. New program
certificates require active access; the automatic backfill path now applies that same access
check and enrollment lock. Issued certificates remain retrievable after access expiry unless
revoked. Revocation is displayed and prevents certificate retrieval through the issue action.
Administrators can inspect certificate status and open public verification; this screen does
not impersonate the student to issue a certificate.

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/student/academic-progress` | Own transcript, semesters and all program reports |
| GET | `/student/collections/{programId}/graduation` | Own original requirements and credit breakdown |
| POST | `/student/collections/{programId}/graduation/check` | Check and record own eligible completion |
| GET | `/admin/students/{userId}/academic-progress` | Student transcript and program reports |
| POST | `/admin/students/{userId}/programs/{programId}/graduation/check` | Recheck using the same academic rules |

Student routes require STUDENT and scope enrollment lookup to the authenticated account.
Administration requires ADMIN or SUPER_ADMIN. Credit records survive loss of study access.
No new migration, environment variables or external services are required for step 6;
it uses the existing V24–V28 schema. Deploy matching API and web images after the prior
migrations. Existing frontend changes are limited to navigation links and replacing the
program certificate action with the graduation panel; the report screens and components
are new files. This implementation has not been deployed to a hosted environment.
