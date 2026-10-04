# Date input contract (READY)

Verified on the dedicated 19081 backend running `d4dba66e94110ce3c3faec218e025d999763ba06` on 2026-10-05. The FE 13005 origin passed CORS preflight against this server.

| API | Input fields | Accepted values |
| --- | --- | --- |
| `POST /api/v1/persons`, `PUT /api/v1/persons/{personId}` | `birthday` | Optional ISO calendar date, `1000-01-01` through `9999-12-31` (MySQL `DATE`). Existing update behavior for omitted/null birthday remains. |
| Same Person APIs | `profile.ageReferenceDate`, `firstMetOn`, `lastContactOn`, `importantDates[].date` | Optional ISO calendar dates, `0001-01-01` through `9999-12-31` (JSON storage); `importantDates[].date` remains required when an entry is present. Profile omitted on update keeps the old profile, explicit `null` clears it, and an object replaces it. |
| `POST/PATCH /api/v1/players/certifications/{certificationId}`; `POST /admin/v1/players/{playerId}/certifications/{certificationId}` | `acquiredDate`, `expiresDate` | Optional ISO calendar dates, MySQL `DATE` range. Expiry may be in the future and cannot precede acquisition when both exist. On player PATCH, omitted/null dates preserve the saved values. |
| `POST/PATCH /api/v1/players/hobbies/{hobbyId}`; `POST /admin/v1/players/{playerId}/hobbies/{hobbyId}` | `startedOn` | Optional ISO calendar date, MySQL `DATE` range. On player PATCH, omitted/null preserves the saved value. |
| `POST /api/v1/players/exercises`, `POST /api/v1/players/exercises/{exerciseId}`; `POST /admin/v1/players/{playerId}/exercises[/{exerciseId}]` | `exercisedOn` | Required on create; optional on update (omitted/null keeps the existing date). ISO calendar date, MySQL `DATE` range. |
| `GET /api/v1/players/exercises/search`, `GET /admin/v1/players/{playerId}/exercises/search` | `from`, `to` query parameters | Optional ISO calendar date filters in the MySQL `DATE` range. |
| `POST /api/v1/roles/{roleId}/events`, `PATCH /api/v1/roles/{roleId}/events/{eventId}` | `startsAt`, `endsAt` | Optional ISO offset date-time strings with four-digit years; the resulting instant, converted to the backend JDBC zone `Asia/Seoul`, must fit MySQL `DATETIME` local dates `1000-01-01` through `9999-12-31`. Both present: end may equal start, but not precede it. Future schedules are allowed. |
| `POST /api/v1/guilds/{guildId}/events`, `PATCH /api/v1/guilds/{guildId}/events/{eventId}` | `startsAt`, `endsAt` | Required ISO offset date-time strings with the same range. End must be strictly after start. Future schedules are allowed. |

All calendar dates must be real Gregorian dates. Year `0000`, negative years, expanded years (five or more digits), and invalid days such as non-leap `2025-02-29` are rejected. Date-only values do not accept a time or offset. Schedule values require an offset (`Z` is accepted); the server compares instants, so different offsets are allowed.

Invalid request values return HTTP 400 `application/problem+json` under the existing bad-input/validation error contract, before any mutation or event. Existing ownership and state errors retain their own codes. Media and Collection requests, and direct LifeLog journal requests, currently have no user-supplied date fields; their internal timestamps are outside this contract.
