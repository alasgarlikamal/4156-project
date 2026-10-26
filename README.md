# 4156-project

## Assignments API

Assignments belong to an existing course. The API follows the course and
enrollment CRUD layout, with request/response records and a service/repository
layer.

| Method | Endpoint | Result |
| --- | --- | --- |
| POST | `/api/assignments` | Create an assignment (201) |
| GET | `/api/assignments?courseId=1` | List assignments; course filter is optional |
| GET | `/api/assignments/{id}` | Retrieve an assignment |
| PUT | `/api/assignments/{id}` | Replace all assignment values |
| DELETE | `/api/assignments/{id}` | Delete an assignment (204) |

Example create or replace body:

```json
{
  "courseId": 1,
  "title": "Project proposal",
  "dueDate": "2026-11-01",
  "estimatedWorkHours": 4.5
}
```

All fields are required. Titles must contain text and be at most 255 characters;
course IDs must be positive; estimated hours must be nonnegative. Past deadlines
are allowed so clients can store historical assignments. Lists are ordered by
due date, then ID. Responses include the generated `id` plus the request fields.
Invalid requests return 400; missing assignments or referenced courses return
404. Course deletion is blocked by the database while assignments reference it,
returning 409 through the shared exception handler.

Run tests and Checkstyle with `./mvnw test`. Assignment tests include MockMvc
controller tests, Mockito service tests, and an in-memory SQLite CRUD test.

## Course sessions and calendar export

Weekly course meetings belong to a course and use ISO weekday numbers
(Monday = 1 through Sunday = 7). Times use `HH:mm` local time in
`America/New_York`. Create and manage sessions at
`/api/courses/{courseId}/sessions`; download all occurrences between the
course's inclusive term dates from `/api/courses/{courseId}/calendar.ics`.
The calendar export expands each weekly session to dated events and encodes
the resulting instants in UTC.

Example session body:

```json
{
  "weekday": 1,
  "startTime": "14:00",
  "endTime": "15:15"
}
```
