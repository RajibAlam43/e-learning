# Admin student directory API

`GET /admin/users` requires an authenticated account with the `ADMIN` role.
It lists accounts with the `STUDENT` role directly, without requiring orders or email
addresses. Additional roles do not exclude an account: a teacher or admin who also
has `STUDENT` is included. Accounts without `STUDENT` are excluded. Teachers also
remain available through `GET /admin/instructors`.
All account statuses (`ACTIVE`, `SUSPENDED`, `DELETED`) are included.

| Query parameter | Default | Behavior |
| --- | --- | --- |
| `search` | Empty | Matches a literal substring of full name, email, or phone; name/email are case-insensitive. Surrounding whitespace is removed. Missing/blank search lists all students. |
| `page` | `0` | Zero-based page number, at least zero. |
| `size` | `20` | Page size between 1 and 100. |

Invalid pagination returns HTTP 400. Results are ordered by `createdAt` descending,
then `userId` descending to break ties. Search and pagination run in the database;
role names are loaded in a batch for the returned page. The Student role filter is
always applied; there is no configurable role/status filter or user mutation.

Example:

```http
GET /admin/users?search=01712&page=0&size=20
Authorization: Bearer <admin-access-token>
```

Response uses the existing `PageResponse` contract:

```json
{
  "content": [
    {
      "userId": "ca34ce8c-3ef8-4cdf-952c-343f11094e57",
      "fullName": "Example Student",
      "email": null,
      "phone": "01712345678",
      "phoneCountryCode": "+880",
      "roles": ["STUDENT"],
      "status": "ACTIVE",
      "createdAt": "2026-11-26T00:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

`email`, `phone`, and `phoneCountryCode` may be null. Roles are sorted by name and
always include `STUDENT`. No matches return empty
`content`, zero `totalElements`, and zero `totalPages`. Pages beyond the last page
return empty `content` while retaining the matching totals. Passwords, tokens, and
Google identifiers are never part of the response. Responses use `Cache-Control: no-store`.

The admin Students screen at `/admin/students` uses this endpoint, with a debounced
name/email/phone search and server-side pagination (20 students per page). The sidebar
and mobile navigation label it Students; Teachers remains a separate view. It shows
account details instead of order totals, and search changes reset to the first page.
Order reporting elsewhere still uses the existing orders endpoint.
No database migration or new deployment environment variables are required.
