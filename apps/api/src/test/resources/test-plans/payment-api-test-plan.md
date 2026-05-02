# PaymentApi Test Plan

## POST `/checkout/courses/{courseId}`
### A. What this API should accomplish
- Create a payable pending order for an eligible learner and avoid duplicate pending checkouts.
### B. Steps this API should follow
1. Resolve current user from authentication.
2. Load course by `courseId`.
3. Verify course is `PUBLISHED`.
4. Verify user is not already actively enrolled.
5. Look up unexpired pending orders for same user+course and reuse if present.
6. Otherwise create `orders` row (`status=PENDING`) and matching `order_items` row.
7. Return checkout summary with expiry metadata.
### C. API tests
- creates pending order for published course.
- reuses still-valid pending order for same user+course.
- returns 409 when user already has active enrollment.
### D. DataJpa tests
- `findByUserIdAndStatus`.
- `findByIdAndUserId`.
### E. Important/Critical validations to include
- do not create duplicate pending rows for same course when prior order is valid.

## GET `/checkout/orders/{orderId}`
### A. What this API should accomplish
- Return payment status and next-action for the order owner.
### B. Steps this API should follow
1. Resolve current user id.
2. Load order by `(orderId,userId)` ownership scope.
3. Load order items and active enrollments for owned courses.
4. Compute `coursesEnrolled`, `enrolledCourseCount`, `nextAction`, and message.
### C. API tests
- after successful payment callback, returns `PAID` and dashboard next-action.
### D. DataJpa tests
- ownership lookup via `findByIdAndUserId`.
### E. Important/Critical validations to include
- no cross-user order visibility.

## POST `/payments/{orderId}/initiate`
### A. What this API should accomplish
- Start provider payment session only for the owner of a valid pending order.
### B. Steps this API should follow
1. Resolve current user id.
2. Load order by `(orderId,userId)`.
3. Verify order status is `PENDING`.
4. Verify order has not expired.
5. Persist selected provider and generated provider transaction/session id.
6. Return payment gateway/redirect metadata.
### C. API tests
- initiating with provider updates provider/session and returns redirect metadata.
### D. DataJpa tests
- `findByProviderAndProviderTxnId`.
### E. Important/Critical validations to include
- reject non-pending and expired orders with 400.

## GET `/payments/{orderId}/success|failed|cancelled`
### A. What this API should accomplish
- Record callback event and transition order state idempotently.
### B. Steps this API should follow
1. Load order by `orderId`.
2. Persist payment-event record with callback payload.
3. On `success`: if pending, set `PAID`, stamp `paidAt`, grant enrollments for order items.
4. On `failed`/`cancelled`: if pending, transition to `FAILED`/`CANCELLED`.
5. Return normalized status payload.
### C. API tests
- success callback transitions to paid and creates active enrollment.
### D. DataJpa tests
- `payment_events` saved with provider/event fields.
### E. Important/Critical validations to include
- idempotent handling for repeated success callback on already paid order.
- callbacks should be publicly callable by providers (no role requirement).

## POST `/public/webhooks/payments/sslcommerz|bkash|nagad`
### A. What this API should accomplish
- Validate signatures (supported providers), record webhook events, and acknowledge receipt.
### B. Steps this API should follow
1. Normalize headers.
2. Resolve provider-specific webhook secret.
3. Extract and validate HMAC signature against payload.
4. Resolve optional order by provider transaction id header.
5. Persist `payment_events` row with raw headers+payload and status `RECEIVED`.
6. Return acknowledgment payload.
7. For `nagad`: return explicit “not enabled” acknowledgment (current behavior).
### C. API tests
- sslcommerz valid signature returns acknowledged=true and persists event.
- bkash invalid signature returns 400.
- nagad returns acknowledged=false “not enabled”.
### D. DataJpa tests
- `findByProviderAndProviderEventId`.
- uniqueness of `(provider, provider_event_id)` when non-null.
### E. Important/Critical validations to include
- webhooks should remain public (provider calls without app user authentication).

## GET `/student/orders/{orderId}/receipt`
### A. What this API should accomplish
- Return receipt details for owner when order is paid/refunded.
### B. Steps this API should follow
1. Resolve current user id.
2. Load order by `(orderId,userId)`.
3. Ensure status is `PAID` or `REFUNDED`.
4. Load order items and compute subtotal/discount/final totals.
5. Build receipt number and return receipt payload.
### C. API tests
- paid order returns receipt with line item course details.
### D. DataJpa tests
- order items retrieval by `orderId`.
### E. Important/Critical validations to include
- block receipt generation for pending/failed/cancelled orders.
