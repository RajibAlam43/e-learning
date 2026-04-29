Yes — with your feedback, the API list becomes much cleaner.

Key changes:

* No separate category/language/level APIs.
* Course search handles filters.
* No FAQ/contact/static content APIs.
* Instructor dashboard is very limited.
* Live classes added.
* Support tickets only create an email-based support request.
* Notification APIs are not MVP unless you want in-app notifications.
* Certificate check + issue can be combined.

Based on the MVP direction and architecture discussion: public browsing, accounts, course purchase, lessons, progress, quizzes, certificates, payments, live classes, and admin operation are the core pieces.

# 1. Public Course APIs

| API                                  | Main function                                                                                 |
| ------------------------------------ | --------------------------------------------------------------------------------------------- |
| `GET /api/public/courses/search`     | Search/filter published courses by keyword, category, level, language, price/free, instructor |
| `GET /api/public/courses/{slug}`     | Get public course detail: description, sections, lessons preview, instructor, price           |
| `GET /api/public/instructors`        | List visible instructors                                                                      |
| `GET /api/public/instructors/{slug}` | Show instructor profile and their published courses                                           |

Course structure should be:

`Course → Sections → Lessons → Video + Resources`

Each lesson has:

* at most one video
* zero or more resources: PDF, image, notes, etc.

---

# 2. Auth & Account APIs

| API                                     | Main function                              |
| --------------------------------------- | ------------------------------------------ |
| `POST /api/public/auth/register`        | Create account                             |
| `POST /api/public/auth/login`           | Login                                      |
| `POST /api/public/auth/refresh`         | Refresh access token using HttpOnly cookie |
| `POST /api/public/auth/logout`          | Logout and revoke refresh token            |
| `GET /api/me`                           | Get logged-in user profile                 |
| `PATCH /api/me/profile`                 | Update profile                             |
| `POST /api/public/auth/forgot-password` | Request password reset                     |
| `POST /api/public/auth/reset-password`  | Reset password                             |

## Phone Verification APIs

| API                                       | Main function                              |
| ----------------------------------------- | ------------------------------------------ |
| `POST /api/public/auth/phone/send-code`   | Send OTP/code to phone number              |
| `POST /api/public/auth/phone/verify-code` | Verify OTP/code                            |
| `POST /api/public/auth/phone/resend-code` | Resend OTP/code                            |
| `PATCH /api/me/phone`                     | Add/change phone number for logged-in user |

For MVP, you can require phone verification either during registration or before payment.

---

# 3. Student Dashboard APIs

| API                                   | Main function                                             |
| ------------------------------------- | --------------------------------------------------------- |
| `GET /api/student/dashboard`          | Show enrolled courses, progress, certificates             |
| `GET /api/student/courses`            | List student’s enrolled/purchased courses                 |
| `GET /api/student/courses/{courseId}` | Get enrolled course home with sections, lessons, progress |
| `GET /api/student/orders`             | List purchase history                                     |
| `GET /api/student/certificates`       | List earned certificates                                  |

---

# 4. Learning / Lesson APIs

| API                                           | Main function                                          |
| --------------------------------------------- | ------------------------------------------------------ |
| `GET /api/learn/lessons/{lessonId}`           | Get lesson content: notes, resources, access info      |
| `GET /api/learn/lessons/{lessonId}/playback`  | Return YouTube or Mux playback info after access check |
| `POST /api/learn/lessons/{lessonId}/progress` | Save progress: completed, last watched position        |
| `POST /api/learn/lessons/{lessonId}/complete` | Mark lesson complete                                   |
| `GET /api/learn/courses/{courseId}/progress`  | Get course progress percentage                         |

---

# 5. Lesson Resource APIs

| API                                                  | Main function                           |
| ---------------------------------------------------- | --------------------------------------- |
| `GET /api/learn/lessons/{lessonId}/resources`        | List lesson PDFs/images/files           |
| `GET /api/learn/resources/{resourceId}/download-url` | Return signed R2 URL after access check |

---

# 6. Quiz APIs

| API                                                | Main function                      |
| -------------------------------------------------- | ---------------------------------- |
| `GET /api/learn/quizzes/{quizId}`                  | Get quiz questions                 |
| `POST /api/learn/quizzes/{quizId}/attempts`        | Start quiz attempt                 |
| `POST /api/learn/quiz-attempts/{attemptId}/submit` | Submit answers and calculate score |
| `GET /api/learn/quizzes/{quizId}/attempts`         | View student’s quiz attempts       |
| `GET /api/learn/quiz-attempts/{attemptId}`         | View attempt result                |

---

# 7. Certificate APIs

Yes, combine check + issue.

| API                                                      | Main function                                                                                     |
| -------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| `POST /api/student/courses/{courseId}/certificate`       | Check eligibility; if eligible, issue certificate; if already issued, return existing certificate |
| `GET /api/student/certificates/{certificateId}/download` | Download certificate PDF                                                                          |
| `GET /api/public/certificates/verify/{code}`             | Public certificate verification                                                                   |

The combined endpoint should be idempotent.

---

# 8. Live Class APIs

Since your architecture includes Zoom/live class style functionality, add these.

## Student Live Class APIs

| API                                                 | Main function                                   |
| --------------------------------------------------- | ----------------------------------------------- |
| `GET /api/student/live-classes`                     | List upcoming live classes for enrolled courses |
| `GET /api/student/courses/{courseId}/live-classes`  | List live classes for one enrolled course       |
| `POST /api/student/live-classes/{liveClassId}/join` | Check enrollment and return join URL            |

## Instructor Live Class APIs

| API                                                     | Main function                         |
| ------------------------------------------------------- | ------------------------------------- |
| `GET /api/instructor/courses`                           | List instructor’s assigned courses    |
| `GET /api/instructor/courses/{courseId}/students/count` | Show number of enrolled students      |
| `POST /api/instructor/courses/{courseId}/live-classes`  | Create/schedule live class            |
| `GET /api/instructor/courses/{courseId}/live-classes`   | List live classes for course          |
| `POST /api/instructor/live-classes/{liveClassId}/start` | Start class and return host/start URL |
| `PATCH /api/instructor/live-classes/{liveClassId}`      | Edit live class time/title            |
| `DELETE /api/instructor/live-classes/{liveClassId}`     | Cancel live class                     |

For MVP, this is enough.

---

# 9. Support API

Yes, you can move actual support conversation to email after ticket creation.

| API                         | Main function                                          |
| --------------------------- | ------------------------------------------------------ |
| `POST /api/support/tickets` | Create support request and send email to support/admin |

No need for:

* ticket messages
* ticket inbox for users
* ticket replies
* ticket status APIs

For MVP, a DB row + email notification is enough.

---

# 10. Notification APIs

I would cut notification APIs from MVP.

Use cases where notification APIs help:

* in-app unread notifications
* “your certificate is ready”
* “live class starts in 30 minutes”
* “payment succeeded”
* “new lesson published”

But for MVP, email/SMS is enough. You can send these from backend/worker without exposing notification APIs.

So cut:

* `GET /api/notifications`
* `PATCH /api/notifications/{id}/read`

---

# 11. Payment APIs

| API                                             | Main function                        |
| ----------------------------------------------- | ------------------------------------ |
| `POST /api/checkout/courses/{courseId}`         | Create pending order                 |
| `GET /api/checkout/orders/{orderId}`            | Get order/payment status             |
| `POST /api/payments/{orderId}/initiate`         | Start bKash/SSLCommerz/Nagad payment |
| `GET /api/payments/{orderId}/success`           | Handle success redirect              |
| `GET /api/payments/{orderId}/failed`            | Handle failed redirect               |
| `GET /api/payments/{orderId}/cancelled`         | Handle cancelled redirect            |
| `POST /api/public/webhooks/payments/sslcommerz` | SSLCommerz webhook/IPN               |
| `POST /api/public/webhooks/payments/bkash`      | bKash webhook/callback               |
| `POST /api/public/webhooks/payments/nagad`      | Nagad webhook/callback               |
| `GET /api/student/orders/{orderId}/receipt`     | View/download receipt                |

---

# 12. Admin APIs

## Admin Course Management

| API                                            |     MVP? | Main function                     |
| ---------------------------------------------- | -------: | --------------------------------- |
| `GET /api/admin/courses`                       | Required | List all courses                  |
| `GET /api/admin/courses/{courseId}`            | Required | View full course detail           |
| `POST /api/admin/courses`                      | Required | Create course manually/admin-side |
| `PATCH /api/admin/courses/{courseId}`          | Required | Edit course info                  |
| `POST /api/admin/courses/{courseId}/publish`   | Required | Publish course                    |
| `POST /api/admin/courses/{courseId}/unpublish` | Required | Unpublish course                  |
| `DELETE /api/admin/courses/{courseId}`         | Optional | Delete/archive course             |

## Admin Sections / Lessons

| API                                                    |     MVP? | Main function            |
| ------------------------------------------------------ | -------: | ------------------------ |
| `POST /api/admin/courses/{courseId}/sections`          | Required | Add section              |
| `PATCH /api/admin/sections/{sectionId}`                | Required | Edit section             |
| `DELETE /api/admin/sections/{sectionId}`               | Required | Delete section           |
| `POST /api/admin/sections/{sectionId}/lessons`         | Required | Add lesson               |
| `PATCH /api/admin/lessons/{lessonId}`                  | Required | Edit lesson              |
| `DELETE /api/admin/lessons/{lessonId}`                 | Required | Delete lesson            |
| `POST /api/admin/courses/{courseId}/structure/reorder` | Required | Reorder sections/lessons |

## Admin Media / Resources

| API                                            |     MVP? | Main function              |
| ---------------------------------------------- | -------: | -------------------------- |
| `POST /api/admin/media/mux/upload-url`         | Required | Create Mux upload URL      |
| `POST /api/admin/resources/upload-url`         | Required | Create R2 upload URL       |
| `POST /api/admin/lessons/{lessonId}/video`     | Required | Attach one video to lesson |
| `POST /api/admin/lessons/{lessonId}/resources` | Required | Attach PDF/image resources |
| `DELETE /api/admin/resources/{resourceId}`     | Required | Remove resource            |

## Admin Quiz Management

| API                                          |                    MVP? | Main function            |
| -------------------------------------------- | ----------------------: | ------------------------ |
| `POST /api/admin/courses/{courseId}/quizzes` | Required if quizzes MVP | Create course/final quiz |
| `POST /api/admin/lessons/{lessonId}/quizzes` |                Optional | Create lesson quiz       |
| `PATCH /api/admin/quizzes/{quizId}`          |                Required | Edit quiz settings       |
| `POST /api/admin/quizzes/{quizId}/questions` |                Required | Add question             |
| `PATCH /api/admin/questions/{questionId}`    |                Required | Edit question            |
| `DELETE /api/admin/questions/{questionId}`   |                Required | Delete question          |
| `POST /api/admin/quizzes/{quizId}/publish`   |                Required | Publish quiz             |

## Admin Instructor Management

No instructor application needed.

| API                                                               |     MVP? | Main function                    |
| ----------------------------------------------------------------- | -------: | -------------------------------- |
| `POST /api/admin/instructors`                                     | Required | Add selected instructor manually |
| `GET /api/admin/instructors`                                      | Required | List instructors                 |
| `GET /api/admin/instructors/{instructorId}`                       | Required | View instructor                  |
| `PATCH /api/admin/instructors/{instructorId}`                     | Required | Edit instructor profile          |
| `POST /api/admin/courses/{courseId}/instructors/{instructorId}`   | Required | Assign instructor to course      |
| `DELETE /api/admin/courses/{courseId}/instructors/{instructorId}` | Optional | Remove instructor from course    |

## Admin Live Classes

| API                                                |     MVP? | Main function             |
| -------------------------------------------------- | -------: | ------------------------- |
| `GET /api/admin/live-classes`                      | Required | View all live classes     |
| `POST /api/admin/courses/{courseId}/live-classes`  | Required | Create live class         |
| `PATCH /api/admin/live-classes/{liveClassId}`      | Required | Edit live class           |
| `POST /api/admin/live-classes/{liveClassId}/start` | Required | Start live class as admin |
| `DELETE /api/admin/live-classes/{liveClassId}`     | Required | Cancel live class         |

## Admin Users

| API                                      |     MVP? | Main function           |
| ---------------------------------------- | -------: | ----------------------- |
| `GET /api/admin/users`                   | Required | List/search users       |
| `GET /api/admin/users/{userId}`          | Required | View user detail        |
| `PATCH /api/admin/users/{userId}/status` | Required | Suspend/reactivate user |
| `PATCH /api/admin/users/{userId}/roles`  | Optional | Change roles manually   |

## Admin Orders / Payments

| API                                              |     MVP? | Main function                 |
| ------------------------------------------------ | -------: | ----------------------------- |
| `GET /api/admin/orders`                          | Required | List/search orders            |
| `GET /api/admin/orders/{orderId}`                | Required | View order detail             |
| `PATCH /api/admin/orders/{orderId}/status`       | Required | Manually update status/refund |
| `POST /api/admin/orders/{orderId}/grant-access`  | Required | Manually give enrollment      |
| `POST /api/admin/orders/{orderId}/revoke-access` | Required | Revoke course access          |

## Admin Certificates

| API                                         |     MVP? | Main function           |
| ------------------------------------------- | -------: | ----------------------- |
| `GET /api/admin/certificates`               | Optional | List certificates       |
| `GET /api/admin/certificates/{id}`          | Optional | View certificate detail |
| `POST /api/admin/certificates/{id}/reissue` | Optional | Re-generate certificate |
| `POST /api/admin/certificates/{id}/revoke`  | Optional | Revoke certificate      |

For MVP, certificate admin APIs can wait unless you expect mistakes/refunds early.

## Admin Dashboard / Analytics

| API                        |     MVP? | Main function                                 |
| -------------------------- | -------: | --------------------------------------------- |
| `GET /api/admin/dashboard` | Optional | Basic counts: users, courses, orders, revenue |
| `GET /api/admin/analytics` | Optional | Completion/sales analytics                    |

Cut this at first if you want faster MVP.

## Admin Audit / Jobs

| API                               |              MVP? | Main function                  |
| --------------------------------- | ----------------: | ------------------------------ |
| `GET /api/admin/audit-logs`       | Optional but good | See who changed important data |
| `GET /api/admin/jobs`             |          Optional | View failed background jobs    |
| `POST /api/admin/jobs/{id}/retry` |          Optional | Retry failed job               |

Not needed for first MVP unless you build job tracking table early.

---

# MVP Cut Recommendation

For fastest MVP, build only these first:

1. Public course search/detail
2. Auth + phone verification
3. Student enrolled courses
4. Lesson playback + progress
5. Checkout/payment/webhooks
6. Admin course/section/lesson/media management
7. Admin instructor creation/assignment
8. Instructor live class create/start
9. Student live class join
10. Certificate combined issue endpoint
11. Basic quiz flow, only if certificates depend on quizzes


You’re right on all 3 points.

## 1. Public courses

Use this as the “all courses” API:

| API                       | Function                                                 |
| ------------------------- | -------------------------------------------------------- |
| `GET /api/public/courses` | List all published courses, with optional filters/search |

Example query:

```http
GET /api/public/courses?keyword=quran&category=tajweed&level=beginner&language=bn&priceType=paid&page=0&size=12
```

So you don’t need both:

```http
GET /api/public/courses
GET /api/public/courses/search
```

Just one flexible endpoint.

---

## 2. Account APIs should include both email + phone

Keep both.

| API                              | Function                           |
| -------------------------------- | ---------------------------------- |
| `POST /api/public/auth/register` | Register with email/phone/password |
| `POST /api/public/auth/login`    | Login using email or phone         |
| `POST /api/public/auth/refresh`  | Refresh access token               |
| `POST /api/public/auth/logout`   | Logout                             |
| `GET /api/me`                    | Get current user                   |
| `PATCH /api/me/profile`          | Update name/profile info           |

### Email verification

| API                                       | Function                     |
| ----------------------------------------- | ---------------------------- |
| `POST /api/public/auth/email/send-code`   | Send email verification code |
| `POST /api/public/auth/email/verify-code` | Verify email code            |
| `POST /api/public/auth/email/resend-code` | Resend email code            |

### Phone verification

| API                                       | Function         |
| ----------------------------------------- | ---------------- |
| `POST /api/public/auth/phone/send-code`   | Send phone OTP   |
| `POST /api/public/auth/phone/verify-code` | Verify phone OTP |
| `POST /api/public/auth/phone/resend-code` | Resend phone OTP |

### Password reset

| API                                     | Function                               |
| --------------------------------------- | -------------------------------------- |
| `POST /api/public/auth/forgot-password` | Send reset code/link to email or phone |
| `POST /api/public/auth/reset-password`  | Reset password                         |

You could combine send/resend:

```http
POST /api/public/auth/email/code
POST /api/public/auth/phone/code
```

Then the backend decides whether it is first send or resend.

---

## 3. Admin APIs can definitely be reduced

Yes — for MVP, you can combine course/section/lesson/media/resource creation.

Since video is uploaded manually in Mux dashboard, your backend only needs to store media metadata.

## Reduced Admin Course APIs

### Courses

| API                                            | Function                        |
| ---------------------------------------------- | ------------------------------- |
| `GET /api/admin/courses`                       | List all courses                |
| `POST /api/admin/courses`                      | Create draft course             |
| `GET /api/admin/courses/{courseId}`            | Get full editable course detail |
| `PATCH /api/admin/courses/{courseId}`          | Update course info              |
| `POST /api/admin/courses/{courseId}/publish`   | Publish course                  |
| `POST /api/admin/courses/{courseId}/unpublish` | Unpublish course                |

No delete needed for MVP. Use `archived` status later.

---

### Sections

| API                                           | Function       |
| --------------------------------------------- | -------------- |
| `POST /api/admin/courses/{courseId}/sections` | Add section    |
| `PATCH /api/admin/sections/{sectionId}`       | Update section |
| `DELETE /api/admin/sections/{sectionId}`      | Delete section |

---

### Lessons

Create lesson with video + resources in one request.

| API                                                    | Function                                                |
| ------------------------------------------------------ | ------------------------------------------------------- |
| `POST /api/admin/sections/{sectionId}/lessons`         | Create lesson with video metadata and resource metadata |
| `PATCH /api/admin/lessons/{lessonId}`                  | Update lesson, video metadata, and resources            |
| `DELETE /api/admin/lessons/{lessonId}`                 | Delete lesson                                           |
| `POST /api/admin/courses/{courseId}/structure/reorder` | Reorder sections/lessons                                |

Example create lesson request:

```json
{
  "title": "Introduction to Tajweed",
  "position": 1,
  "isPreviewFree": true,
  "notesHtml": "<p>Lesson notes...</p>",
  "video": {
    "provider": "MUX",
    "muxAssetId": "abc123",
    "muxPlaybackId": "xyz789",
    "playbackPolicy": "SIGNED",
    "durationSec": 600
  },
  "resources": [
    {
      "type": "PDF",
      "title": "Class Notes",
      "storageKey": "courses/tajweed/lesson-1-notes.pdf"
    },
    {
      "type": "IMAGE",
      "title": "Makharij Diagram",
      "storageKey": "courses/tajweed/makharij.png"
    }
  ]
}
```

Then `PATCH /api/admin/lessons/{lessonId}` can replace/update:

* title
* notes
* video metadata
* resources list
* preview status
* position/status

---

### Optional shared media endpoint

Since you may add media manually from Mux dashboard, one lightweight endpoint is useful:

| API                            | Function                                       |
| ------------------------------ | ---------------------------------------------- |
| `POST /api/admin/media-assets` | Save reusable Mux/YouTube media metadata in DB |

Then lesson creation can either include direct video metadata or reference:

```json
{
  "mediaAssetId": "uuid-here"
}
```

For MVP, I’d choose one approach:

**Simplest:** no separate media endpoint; add video metadata directly during lesson creation/update.

**Cleaner long-term:** keep `POST /api/admin/media-assets`.

---

## Reduced Admin Instructor APIs

| API                                              | Function                         |
| ------------------------------------------------ | -------------------------------- |
| `GET /api/admin/instructors`                     | List instructors                 |
| `POST /api/admin/instructors`                    | Add selected instructor manually |
| `PATCH /api/admin/instructors/{instructorId}`    | Update instructor                |
| `POST /api/admin/courses/{courseId}/instructors` | Assign/update course instructor  |

For MVP, one course can have one primary instructor. So instead of add/remove instructor APIs, just update assignment:

```http
POST /api/admin/courses/{courseId}/instructors
```

Body:

```json
{
  "instructorId": "uuid-here"
}
```

---

## Reduced Admin Live Class APIs

| API                                                | Function                             |
| -------------------------------------------------- | ------------------------------------ |
| `GET /api/admin/live-classes`                      | List all live classes                |
| `POST /api/admin/courses/{courseId}/live-classes`  | Create/schedule live class           |
| `PATCH /api/admin/live-classes/{liveClassId}`      | Update/cancel/start status if needed |
| `POST /api/admin/live-classes/{liveClassId}/start` | Start class and return host URL      |

You can skip separate cancel/delete and just use PATCH:

```json
{
  "status": "CANCELLED"
}
```

---

## Reduced Admin Quiz APIs

For MVP, combine quiz + questions.

| API                                          | Function                           |
| -------------------------------------------- | ---------------------------------- |
| `POST /api/admin/courses/{courseId}/quizzes` | Create quiz with questions/choices |
| `PATCH /api/admin/quizzes/{quizId}`          | Update quiz, questions, choices    |
| `POST /api/admin/quizzes/{quizId}/publish`   | Publish quiz                       |

No separate question APIs needed at first.

---

## Reduced Admin Payment APIs

| API                                 | Function                                           |
| ----------------------------------- | -------------------------------------------------- |
| `GET /api/admin/orders`             | List/search orders                                 |
| `GET /api/admin/orders/{orderId}`   | View order detail                                  |
| `PATCH /api/admin/orders/{orderId}` | Update status, grant/revoke access, add admin note |

One `PATCH` can handle manual payment actions.

---

## Reduced Admin Certificate APIs

Optional for MVP.

| API                                             | Function                          |
| ----------------------------------------------- | --------------------------------- |
| `GET /api/admin/certificates`                   | List certificates                 |
| `PATCH /api/admin/certificates/{certificateId}` | Reissue/revoke/update certificate |

But you can cut this from first MVP.

---

## Final principle

For MVP, prefer fewer “whole object” APIs:

```http
POST lesson with video + resources
PATCH lesson with video + resources
POST quiz with questions + choices
PATCH quiz with questions + choices
PATCH order for manual admin action
PATCH live class for status changes
```

Then later, when the admin UI becomes more advanced, you can split them into smaller APIs.
