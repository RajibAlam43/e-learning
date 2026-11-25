# Google Meet scheduling without Calendar

The LMS uses the generally available **Meet REST API v2** directly. It creates one
unique `spaces/{space}` resource per class, enables Host Management (`moderation: ON`),
uses restricted admission, and assigns all course instructors' Google account emails
as `COHOST` members. Teacher and student links both open the returned `meetingUri`.
OAuth credentials belong to the organizer; teachers do not share that login or grant
the LMS access to their Google accounts. Teachers must join using the Google account
whose email was granted the role.

Google Calendar is no longer called or required. Dates, titles, descriptions,
rescheduling, status and enrollment access remain in the LMS. There are no Calendar
invitations, reminders or Calendar event updates. Meet spaces do not have scheduled
start/end times; a person with a previously obtained Meet link may access it outside
the LMS join window, subject to Google's admission controls. This is also not a Meet
Media API integration. Native recording and Drive ingestion remain a separate phase.
No frontend source or generated frontend API types are changed.

## Organizer setup

1. Enable the Google Meet API in the organizer OAuth app's Google Cloud project.
2. Authorize offline user access with the sensitive scope
   `https://www.googleapis.com/auth/meetings.space.created`. Obtain a new refresh
   token if the old token only granted Calendar access; an existing refresh token
   does not gain scopes by changing application configuration.
3. Supply `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` and `GOOGLE_REFRESH_TOKEN` through
   the existing secret store. `GOOGLE_CALENDAR_ID` and Calendar scopes are no longer
   needed by this code. Public OAuth applications may require Google's scope
   verification; testing users/internal applications must follow Google's applicable
   consent setup. No account consent, infrastructure change or deployment is performed
   by this repository change.
4. The organizer must have a Google edition eligible for co-hosts. Google lists
   Business Standard and other eligible editions; a free personal organizer is not
   a supported co-host setup. Preparing this code does not purchase or enable a plan.
5. Every assigned course instructor must have an LMS email matching their Google
   account. A phone-only instructor must add that email before Google Meet scheduling
   succeeds. Course and manual co-hosts are deduplicated case-insensitively; more than
   25 combined accounts is rejected before role changes are made.

The existing `integrations.google-oauth.*` keys remain. The new base endpoint is
`integrations.google-meet.base-url=https://meet.googleapis.com/v2`. Responses never
include the organizer's access or refresh tokens.

## Scheduling and simultaneous classes

Apply Flyway **V22** using the existing migration release procedure before deploying
API/worker images that share the changed common entity. It adds `lead_instructor_id`
and backfills historical classes using the course's primary teacher (or the first
assigned teacher when no primary exists). It does not call Google or change live data
outside the database migration being explicitly deployed.

For instructor-created classes, the lead is the instructor scheduling it. For
admin-created classes, the lead is the course's primary teacher, falling back to an
assigned teacher. The lead controls double-booking checks; all course teachers still
receive co-host permissions.

Google Meet no longer has an application-wide provider overlap restriction. Distinct
lead teachers can schedule overlapping classes across courses even if they share
assistant co-hosts. A lead cannot be double-booked across Google Meet/Zoom classes.
Adjacent time slots are allowed. PostgreSQL transaction advisory locks serialize
checks for each lead. Zoom retains its separate shared-host overlap guard; this
change does not claim a Zoom account supports simultaneous meetings. Existing Google
account/edition limits and API quotas still apply; validate real concurrent meetings
with the configured organizer before a teacher pilot.

Course memberships are refreshed at creation, on the LMS start operation, and when
the admin co-host endpoint runs. Course assignment changes alone do not immediately
push role changes into an already running meeting. Removed course teachers are removed
from LMS-managed membership during the next refresh unless explicitly retained as
manual co-hosts. Manually managed roles added directly in Google outside the LMS are
preserved. External-URL classes remain managed outside this integration.

## Admin co-host endpoint

`POST /admin/live-classes/{liveClassId}/co-hosts`

Requires existing LMS authentication with `ADMIN` or `SUPER_ADMIN`. It supports an
LMS-created Google Meet class in `SCHEDULED` or `LIVE` status.

- To add the current admin, omit the body or send `{}`. Their LMS email must match
  the Google account they will use. Phone-only admins must supply an explicit email.
- To add another account, send `{"email":"teacher@example.com"}`.
- Success returns `liveClassId`, `email`, `role: "COHOST"`, the effective
  `coHostEmails` list and a direct Meet `joinUrl`.
- Adding an existing email is idempotent. Manual additions are retained separately
  in provider metadata and survive later course-teacher refreshes.
- An invalid email, unsupported class or more than 25 combined accounts is rejected.
  A Google role failure rolls back the LMS metadata change and does not report success.
  Partial role updates attempt compensating Google changes; an external API failure
  during compensation may require organizer reconciliation before retrying.

## Legacy classes and cancellation

Existing Google classes return their direct participant Meet URL as the effective
host URL immediately; old Calendar event URLs are never used as host links.

A **scheduled** legacy API-provisioned class is upgraded to a newly created Meet
space on start or when an admin adds a co-host. The LMS retains `legacyCalendarEventId`
and `legacyMeetUrl` in provider metadata for cleanup. This changes that class's join
link; old Calendar events and old Meet links are not deleted, updated or revoked.
Remove those events/links manually and use the LMS's new link. A currently live
legacy meeting is never silently moved; adding a co-host through this endpoint
returns 409 and must instead be done in the current Google meeting by its organizer.

Cancellation removes LMS-managed co-host memberships, restricts admission and ends
any active conference in the new space. The Meet REST API has no `spaces.delete`;
this does not permanently invalidate a Meet URL or remove unmanaged Google members.
The LMS blocks further student joins for cancelled classes. Legacy Calendar-created
classes cancelled before migration are cancelled in the LMS only; organizer cleanup
of their Calendar events/old meeting access is manual. Failed provisioning can leave
an unused Meet space, since Google does not offer a space-deletion method.

## Validation

Run the API unit suite and the Google Meet/scheduling integration tests:

```
./gradlew :api:test
./gradlew :api:integrationTest --tests '*GoogleMeetSchedulingApiIt' --tests '*InstructorLiveClassesApiIt'
```

HTTP tests exercise the production REST client against a local fake Google server.
Database-backed tests cover migration validation, course teachers, manual/self co-hosts,
authorization, rollback, overlapping leads, shared assistants and legacy upgrades.
These checks do not replace a live Google acceptance test with the eligible organizer.

## Google references (checked November 25, 2026)

- [Meet REST release notes](https://developers.google.com/workspace/meet/release-notes):
  space members became GA September 11, 2026; Meet Media API endpoints are scheduled
  to shut down January 15, 2027.
- [Create a space](https://developers.google.com/workspace/meet/api/reference/rest/v2/spaces/create)
- [Manage space members](https://developers.google.com/workspace/meet/api/guides/meeting-space-members)
- [OAuth authorization](https://developers.google.com/workspace/meet/api/guides/authenticate-authorize)
- [Co-host eligibility and limit](https://support.google.com/meet/answer/10885841?hl=en)
- [End active conference](https://developers.google.com/workspace/meet/api/reference/rest/v2/spaces/endActiveConference)
