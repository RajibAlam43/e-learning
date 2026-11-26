# Google Meet ownership, teacher authorization and Calendar

The default now uses the **course's primary teacher's Google account**. The primary
teacher grants permission once through Google OAuth; the LMS creates classes on their
behalf without their password. An assistant can schedule, but the primary teacher owns
the meeting and is checked for double-booking. Exactly one primary teacher is required
in this mode. Phone-only LMS users can connect a separate verified Google email.

```
GOOGLE_MEET_SHARED_ORGANIZER_ENABLED=false
GOOGLE_MEET_CALENDAR_SYNC_ENABLED=true
```

With these defaults, the supported Calendar API v3 creates each event and its unique
Meet conference on the authorized teacher's **primary** calendar. Other course teachers
are invited as Calendar guests. They are not automatically Meet co-hosts: that feature
requires an eligible organizer edition. Invitation visibility depends on recipients'
Calendar settings. Calendar permissions are limited to `calendar.events.owned`, plus
OpenID/email to verify the connected Google account. Personal accounts retain Google's
normal meeting duration and participant limits; OAuth does not upgrade their plan.

Setting Calendar sync to `false` instead uses Meet REST API v2 directly. Google's Meet
API quickstart specifies a Workspace account; don't assume this path works for ordinary
Gmail accounts. Keep Calendar enabled for the intended free-personal-account workflow.
The default teacher mode makes no co-host/member API calls.

Cloud Identity Free can supply managed Google identities and centralized offboarding,
but it does not include Calendar licensing or upgrade an organizer's Meet entitlement.
It is not a replacement for the Calendar-capable account in the default teacher flow.
The application does not provision Cloud Identity users or use domain-wide delegation.
An admin-managed identity/delegation flow would require separate setup and validation.

## Shared organizer later

After obtaining and authorizing an eligible organizer account:

```
GOOGLE_MEET_SHARED_ORGANIZER_ENABLED=true
GOOGLE_REFRESH_TOKEN=<organizer grant with required scopes>
```

New meetings then use the shared organizer and Meet REST API v2, add all course teachers
as co-hosts (up to 25 combined course/manual accounts), and optionally create a Calendar
event with the direct Meet link and invitations. These mirror events contain a clickable
link, rather than creating another conference. The organizer grant needs
`meetings.space.created` and, when Calendar is enabled, `calendar.events.owned`.
A new consent grant may be required; changing configuration cannot add OAuth scopes.
Native Google recording and Drive ingestion remain separate work.

**The switch affects new meetings only.** Each created meeting stores its organizer mode,
API type and, for teacher-owned meetings, LMS owner ID and verified Google subject.
Start, updates and cancellation use that recorded owner even after switching modes.
Connecting a different Google account does not transfer old meetings; reconnect the
original account when managing those meetings. Keep original teacher grants and shared
credentials available while their meetings remain active. Existing historical spaces
without ownership metadata are treated as shared-organizer spaces.

## OAuth setup and endpoints

Apply migrations through **V23**, then deploy compatible API/worker images using the
existing release process. No deployment, Google account login or cloud setup is performed
by this code change.

1. Configure a Google OAuth **web application** client and external consent screen for
   personal teacher accounts. Enable Calendar API, and Meet API when using REST spaces.
2. Register `GOOGLE_MEET_OAUTH_REDIRECT_URI` exactly with Google. For example:
   `https://api.example.com/public/google-meet/oauth/callback`.
3. Supply `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` and a dedicated base64-encoded random
   **32-byte** `GOOGLE_MEET_TOKEN_ENCRYPTION_KEY` through deployment secrets. Never reuse
   a JWT key or commit a real key. Retain it with encrypted backups; changing/loss of the
   key prevents decrypting stored grants and requires reconnection/key migration.
4. Set `GOOGLE_MEET_OAUTH_RETURN_URL` to the teacher live-classes page on the real frontend.
   Use public HTTPS hosts, trusted proxy configuration, and credentialed API CORS for
   the LMS origin. `GOOGLE_MEET_OAUTH_COOKIE_SECURE=false` is for HTTP localhost only.
5. External apps requesting sensitive scopes may need Google's verification. Apps in
   testing need allowlisted users and can have expiring refresh grants. Use separate
   staging/production clients and secrets. Redact callback query strings, authorization
   headers and token endpoints in proxy/APM logs; don't log Google credentials.

Authenticated teacher/admin endpoints:

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/instructor/google-meet/connection` | Connection email/status and current new-meeting mode |
| POST | `/instructor/google-meet/connection/authorize` | Returns consent URL and sets a browser-binding cookie |
| DELETE | `/instructor/google-meet/connection` | Deletes the local grant; use Google account permissions to revoke it at Google too |

`GET /public/google-meet/oauth/callback` completes consent without an LMS bearer token;
its single-use, ten-minute state is bound to the initiating LMS user and an HttpOnly,
Secure, SameSite=Lax browser cookie. PKCE binds the code exchange; verified Google ID-token
claims, nonce, email verification and granted scope are checked. Refresh tokens and PKCE
verifiers are AES-256-GCM encrypted with per-owner associated data. Tokens never appear
in frontend status responses or meeting metadata. Expired/revoked grants require
reconnecting; missing authorization blocks scheduling with an actionable response.

The teacher live-classes screen now has Connect/Reconnect/Disconnect Google controls.
This is separate from Google login to the LMS; signing into the LMS does not grant
permission to create Calendar events or Meet rooms.

## Admin co-host management and concurrency

`POST /admin/live-classes/{liveClassId}/co-hosts` remains available for LMS-managed
**shared-organizer** meetings. Send `{"email":"person@example.com"}` or omit the body to
add the signed-in admin's email. Requires ADMIN/SUPER_ADMIN. Manual additions survive
teacher membership refreshes. Teacher-owned meetings return 409 rather than pretending
that free-account guests have co-host permissions. Course roles refresh at creation,
on class start and when this endpoint runs; assignment changes don't immediately push
to an active meeting by themselves.

Different lead teachers can host simultaneous Google classes across courses. Shared
assistant co-hosts don't block concurrency; a designated lead cannot be double-booked.
Zoom retains its shared-host limit. The application's permission to schedule does not
establish Google's account concurrency limits; verify actual simultaneous sessions in
the selected organizer's tenant before a teacher pilot.

## Lifecycle and legacy behavior

Teacher-owned Calendar classes keep their original owner/event on reschedule; cancellation
deletes that Calendar event and sends cancellation notices. Deleting an event is not a
promise that a previously distributed Meet link becomes permanently unusable.
Shared Meet-space cancellation removes LMS-managed co-hosts, restricts admission and ends
an active conference, and deletes its optional Calendar mirror. Meet REST has no
`spaces.delete`. The LMS blocks students from cancelled classes.

Historical Calendar-created scheduled classes without ownership metadata are re-created
when started or when adding an admin co-host. Their old event/link references are kept
for manual organizer cleanup; a currently live legacy meeting is never silently moved.
Existing classes always return direct Meet URLs, not Calendar pages, as host links.
Optional Calendar sync can create invitations on start for pre-existing Meet spaces.
Failed cross-service operations attempt cleanup; failed compensation can leave an unused
Meet space/event or role updates and needs organizer reconciliation before retrying.

## Verification

Tests cover browser/state/nonce flow boundaries, encrypted credentials, denied/expired
consent, missing scopes, ownership across a mode switch, primary-teacher selection,
Calendar requests, concurrent scheduling and manual co-host permissions. Local fake
Google servers replace real Google calls. A real teacher must still consent in the
configured Google app and verify meeting ownership, invitations and permissions in a
staging trial before launch.

References checked November 25, 2026:

- [Google OAuth web server flow](https://developers.google.com/identity/protocols/oauth2/web-server)
- [Calendar event creation](https://developers.google.com/workspace/calendar/api/guides/create-events)
- [Meet authorization](https://developers.google.com/workspace/meet/api/guides/authenticate-authorize)
- [Meet API quickstart prerequisites](https://developers.google.com/workspace/meet/api/guides/quickstart/python)
- [Co-host eligibility](https://support.google.com/meet/answer/10885841?hl=en)
- [Meet API release notes](https://developers.google.com/workspace/meet/release-notes)
- [Cloud Identity licensing](https://docs.cloud.google.com/identity/docs/how-to/how-licensing-works-for-cloud-identity)
