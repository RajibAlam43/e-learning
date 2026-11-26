# Google OAuth verification

Prepared for Global Islamic Institute, Bangladesh, on November 25, 2026.
These instructions do not submit the app or publish a release. Review the policies
and complete a real Google staging trial before requesting verification.

## Branding and public pages

Use these values in Google Auth Platform → Branding after deploying the reviewed pages:

| Field | Value |
| --- | --- |
| App name | Global Islamic Institute |
| User support email | rajibalam@globalislamicinstitute.com |
| Homepage | https://globalislamicinstitute.com |
| Privacy Policy | https://globalislamicinstitute.com/privacy-policy |
| Terms of Service | https://globalislamicinstitute.com/terms |
| Authorized domain | globalislamicinstitute.com |
| Developer contact | rajibalam@globalislamicinstitute.com |

The website also provides https://globalislamicinstitute.com/cookie-policy.
The existing footer links use these local pages when no custom policy URL is set.
Existing signup Terms/Privacy links use the same defaults. Custom URLs in `site.legal`
continue to take precedence; inspect those settings before submission. No signup
layout or additional age field is introduced. The Terms state the minimum age of 13.

Verify domain ownership through Google Search Console with a Google account that has
the appropriate project access. Keep branding, logo, website identity and consent
screen identity consistent. Public policy URLs must work without signing in.
Use an external audience when teachers use accounts outside the organization.

## Scopes

| Permission | Purpose |
| --- | --- |
| `openid` | Identify the consenting Google account |
| `https://www.googleapis.com/auth/userinfo.email` (`email`) | Verify and link the connected account's email |
| `https://www.googleapis.com/auth/userinfo.profile` (`profile`) | Basic profile information returned by Google sign-in, where requested by the sign-in client |
| `https://www.googleapis.com/auth/calendar.events.owned` | Create and maintain class events on calendars the authorized host owns |
| `https://www.googleapis.com/auth/meetings.space.created` | Create and manage app-created Meet spaces and eligible co-host assignments |

Include Calendar and Meet in the verification submission for the implemented features
you will demonstrate. Declare only identity scopes actually used by the configured
Google sign-in client. Teachers authorize scheduling separately from LMS sign-in.
Calendar-enabled teacher connections request `openid email` plus Calendar; disabling
Calendar selects the Meet scope instead. Shared organizer credentials need Meet and,
if enabled, Calendar. These modes are documented in [google-meet.md](google-meet.md).

Do not add Gmail, broad Calendar access, Drive access or Meet read-all scopes for this
implementation. Browser recording uses browser capture and private uploads; it does
not download Google's native recordings. Scope declarations do not upgrade existing
refresh tokens: obtain a new consent grant when the required permissions change.

### Calendar justification

> Global Islamic Institute lets teachers schedule their course's live classes from
> the LMS. The authorized host's primary calendar receives a class event and a Google
> Meet conference. The LMS invites the other course teachers, updates the event when
> a class is rescheduled and deletes it when the class is cancelled. In shared
> organizer mode, optional Calendar events contain the corresponding Meet link and
> teacher invitations. We use calendar.events.owned rather than full Calendar access
> because these operations concern events on calendars owned by the consenting host.
> Our product does not display unrelated personal calendar events.

### Meet justification

> Global Islamic Institute creates Meet spaces for its live classes and manages
> settings and metadata for spaces created by this application. In shared organizer
> mode, it assigns course teachers and explicitly designated accounts as co-hosts
> where the organizer's Google edition permits this. It manages these assignments
> and the class meeting lifecycle using meetings.space.created. It does not need
> access to arbitrary meetings, participant history, Gmail or Drive files. The scope
> is limited to app-created spaces and is used only for the disclosed class features.

## Demonstration and submission

1. Enable Google Calendar API and Google Meet API in the project. Check the OAuth web
   client, public HTTPS redirect URI and authorized JavaScript origin for Google sign-in.
   Do not put a client secret or refresh token into frontend configuration.
2. Review the published policies and actual production settings. Confirm that each
   disclosure matches enabled features and that policy links are accessible.
3. Record an end-to-end demonstration of Google sign-in and separate teacher scheduling
   authorization. Show the app identity, full consent flow and requested permissions,
   then create, reschedule and cancel a class and show its Calendar event and Meet link.
4. To include Meet scope verification, demonstrate the direct Meet path with an eligible
   account. For shared co-host features, use the licensed organizer, show teacher/admin
   co-host assignment and the corresponding Meet result. Do not present mocked tests
   or Calendar invitations as proof that co-host assignment worked.
5. Show Disconnect and explain separate Google-side revocation and data deletion
   requests. Keep access tokens, refresh tokens and client secrets out of the video.
6. Add the scope justifications and accessible demonstration video in Google Auth
   Platform's Verification Center. Supply reviewer access through Google's requested
   process and monitor the developer contact inbox for follow-up.

If the licensed Meet path is not yet testable, finish that demonstration before
submitting it as a supported feature. Google's review determines approval.

References checked November 25, 2026:

- [Sensitive-scope verification](https://developers.google.com/identity/protocols/oauth2/production-readiness/sensitive-scope-verification)
- [Meet authorization scopes](https://developers.google.com/workspace/meet/api/guides/authenticate-authorize)
- [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy)

## Transactional email previews

Verification and password-reset emails have a branded HTML body plus the original
plain-text fallback. The API includes the actual OTP expiry in new queue messages;
older queued messages retain their original wording. Run `./gradlew :worker:test`
to generate `apps/worker/build/email-previews/verification.html` for local inspection.
Tests mock SES and do not send email. The logo and policy links use public production
URLs and should be checked after the reviewed release is deployed.
