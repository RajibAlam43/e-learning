# Live-class recording backend pilot

This change adds backend support for recordings produced by a browser webpage or a
Chrome extension. `method` records which client produced each recording; both use
the same upload, validation and publication flow. **No recorder frontend or extension
is included or deployed.** Teachers cannot record through the current LMS UI yet.

Recordings use a dedicated private AWS S3 bucket and signed CloudFront delivery.
Existing R2 storage, Mux lesson videos and meeting scheduling are unchanged. This
does not enable Google Meet's native recording. The separate scheduling changes in
`docs/google-meet.md` cover co-host roles and concurrent classes.

## Recording contract

- The client must mix the selected Meet tab audio and the teacher microphone into
  one AAC audio track, plus H.264 video in MP4. Backend validation cannot determine
  whether both sources were audible or recording consent was obtained.
- Check actual recording capability on the teacher's device, including a short
  sample. Requiring Chrome alone does not guarantee H.264/AAC encoding support.
- Canonical MIME examples: `video/mp4;codecs=avc1,mp4a.40.2` or
  `video/mp4;codecs=avc1.42E01E,mp4a.40.2`. Do not send quotes/spaces in this field.
- Upload complete MP4 bytes in order, grouped into 8 MiB S3 parts. Only the last
  part may be smaller. MediaRecorder's periodic blobs are not standalone HLS
  segments; preserve all original bytes, including initialization data.
- Limits: 8 GiB, 1,024 parts, eight hours, up to 1920×1080, at least one second,
  H.264 video and AAC audio. The intended client recording setting is 720p.
- Configure regular keyframes in the eventual capture client where supported.
  HLS targets six-second segments, but stream copying cuts at existing keyframes;
  long gaps between keyframes produce longer segments and slower seeking/startup.
- Preserve the recording and upload state in durable local storage until completion
  succeeds. Retrying a part replaces that same part number; do not allocate a new
  recording session for a network retry. Never log tokens or signed URLs.

## APIs

Authenticated endpoints use the existing LMS bearer token. Upload endpoints use a
separate random `X-Recording-Token`, scoped to one recording and valid for 24 hours.
Only its SHA-256 hash is stored in PostgreSQL. Each upload operation checks that the
owner is still assigned to the class. Students cannot create sessions.

| Method and path | Purpose |
| --- | --- |
| `GET /instructor/live-classes/{classId}/recordings` | Assigned teacher/admin: configuration flag, meeting URL and all recording statuses |
| `POST /instructor/live-classes/{classId}/recordings` | Assigned teacher: start with `{"method":"BROWSER","mimeType":"video/mp4;codecs=avc1,mp4a.40.2"}`; use `EXTENSION` for the second client |
| `POST /instructor/recordings/{id}/resume` | Original owner: renew upload token; invalidates the old token |
| `POST /recording-uploads/{id}/parts/{number}?bytes={size}` | Scoped upload token: obtain a ten-minute S3 PUT URL, signed for the part's exact byte size |
| `POST /recording-uploads/{id}/complete?parts={count}&bytes={total}` | Scoped upload token: verify S3 parts, complete upload, verify object size and queue validation; safe to retry |
| `POST /recording-uploads/{id}/abort` | Scoped upload token: abort an unfinished multipart upload |
| `GET /live-classes/{classId}/recordings` | Assigned teacher/admin or active enrolled student: list READY recordings |
| `GET /recordings/{id}/playback` | Authorize playback; return HLS `url`, signed MP4 `fileUrl`, `expiresAt` and three CloudFront signed cookies |

Session responses include `id`, `uploadToken`, `expiresAt`, `mimeType`,
`partSizeBytes` and `maxBytes`. Send the returned S3 PUT URL the exact raw bytes of
that part, with `credentials: 'omit'`; the browser sets Content-Length. Do not send
LMS authorization, cookies or the recording token to S3. Upload control requests
also use `credentials: 'omit'`, and the recording token in its own header.

Statuses are `UPLOADING → QUEUED → PROCESSING → READY`, or `FAILED`. A recording is
available to authorized students immediately after the worker marks it READY.
Publication is an API state change; this does not add buttons, notifications or
recording links to existing frontend responses. The eventual clients must poll
status and request playback through these APIs.

## Worker processing and cost

The worker claims jobs from PostgreSQL using `FOR UPDATE SKIP LOCKED`; no additional
SQS queue is needed. One job runs at a time per worker instance. A two-hour claim
lease lets another worker recover after a crash. Processing retries up to three
claims; a claim UUID prevents a stale worker from marking another worker's job READY.

The worker downloads the completed object, checks its size and probes the media.
Unsupported codecs are rejected rather than converted. FFmpeg uses `-c copy` for
both single-quality fragmented-MP4 HLS and a fast-start MP4 fallback, uploads all
outputs, then sets READY. It never calls a paid encoding API or creates extra
quality renditions. This uses disk, network and CPU; it is not a promise of free
infrastructure. The MP4 fallback adds approximately one additional recording's
worth of storage, in addition to the original and HLS copy.
Generated files above 128 MiB use bounded 8-MiB multipart uploads, including MP4
fallbacks that exceed S3's single-upload limit. AWS storage, delivery and requests,
and source downloads to a worker hosted outside AWS, still incur their usual costs.

The worker image installs FFmpeg/FFprobe. Native workers require them on PATH or
the configured binary paths. Reserve at least four times the source size plus
1 GiB of free temporary disk (about 33 GiB for an 8-GiB source). Failed jobs retain
their S3 original for diagnosis/recovery; no existing objects are deleted.

## Deployment configuration

Use `deploy/env/recordings.env.example`. Apply V21 using the existing migration
release procedure, deploy API and worker together, then enable the feature. No
AWS resources are provisioned by this change. Defaults leave the feature disabled.

1. Create a separate private S3 bucket in the selected region. Enable Block Public
   Access, bucket-owner-enforced ownership and server-side encryption. Configure
   lifecycle cleanup of incomplete multipart uploads after two days. Choose a
   separate retention policy for originals and generated playback files.
2. Give API credentials `s3:PutObject`, `s3:GetObject`,
   `s3:ListMultipartUploadParts` and `s3:AbortMultipartUpload`, scoped to
   `arn:aws:s3:::BUCKET/recordings/*`. Give worker credentials `s3:GetObject` for
   originals and `s3:PutObject` for generated recordings. With SSE-KMS, add the
   corresponding KMS grants. Default AWS credentials/IAM roles are supported when
   recording-specific keys are blank. Do not reuse broad administrator keys.
3. Configure S3 CORS for exact LMS origins (and the extension origin when known):
   methods PUT, allowed request headers `*`, exposed response header `ETag`.
   The extension also needs narrow host permissions for the API and S3 endpoint.
4. Create CloudFront with S3 Origin Access Control. Its bucket policy must allow
   GetObject only for that distribution's ARN. Require a trusted key group on the
   entire recording path, including HLS playlists, initialization files, media
   segments and MP4 fallback. Keep direct S3 reads private. Use HTTPS only.
5. Set a custom media domain on the same controlled parent domain as the API,
   such as `recordings.globalislamicinstitute.com` and
   `api.globalislamicinstitute.com`. Configure a valid TLS certificate, signing
   public key/key group and API-only private key. The example domain is illustrative;
   stage must use its own bucket, distribution and signing configuration.
6. Configure CloudFront CORS with exact LMS origins, GET/HEAD/OPTIONS and credentials
   allowed. The HLS player must send credentials for every playlist/segment request
   (`xhr.withCredentials = true`, or native video `crossorigin="use-credentials"`).
   Do not include signed cookies in the cache key or forward them to S3. Keep
   publication/list/playback authorization responses uncached.
7. The API checks that its public hostname and the media hostname both match
   `RECORDINGS_CLOUDFRONT_COOKIE_DOMAIN`; configure trusted proxy forwarding correctly.
   An unrelated default `cloudfront.net` hostname cannot receive these cookies.

Upload-control endpoints deliberately allow cross-origin capability-token requests
without cookies, so an installed extension does not receive general LMS credentials.
All other API CORS and authentication behavior remains unchanged. HLS cookies are
Secure, HttpOnly, SameSite=Lax and restricted to one recording's HLS path. The MP4
fallback uses an exact-resource signed URL. Both expire after enough time for the
recording plus a margin (two to ten hours); request new playback authorization when
expired. Signed access controls distribution but is not DRM.

## Verification and next phase

Run `./gradlew :api:test :worker:test`, then the targeted Postgres integration test
`./gradlew :api:integrationTest --tests '*RecordingApiIt'`. Tests cover both method
labels, teacher assignment, scoped token expiry/renewal/revocation, completion
retries, multipart boundaries, unpublished media, enrollment checks, codec rejection
and CloudFront signatures.

Before enabling a real pilot, validate a representative Chrome recording through
the configured S3/CloudFront stack: confirm both audio sources, seek in HLS and MP4,
reject unsigned URLs/direct S3 access, and test authorized/expired enrollment and
upload interruption. Browser/extension capture, teacher installation, student UI
integration and real teacher trials are explicitly deferred by the backend-only scope.
