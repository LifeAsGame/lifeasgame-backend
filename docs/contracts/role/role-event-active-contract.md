# RoleEvent personal schedule contract

Status: **ready on dedicated 19081; MySQL and real HTTP verified 2026-10-02** (2026-10-02). Target base: `origin/develop` `7520f3a99c3e234116f242d2f299a6d884811f9c`. Implementation SHA: `2ae39dcc2ae9b24d884c4c0c499123624b17945d`. This contract supersedes the old CFC-EVT-001 command gate only for private RoleEvent commands.

All routes require authentication and use the current **Player**. `roleId` and `eventId` identify the player's own Role and event; the client never sends `playerId`. A participant is either `PERSON` (an active Person owned by that Player, `participantId` is Person ID) or `SERVICE_USER` (an active service User ID). A service User reference is a private record: it grants no invitation, access, notification, or membership.

| Method | Path | Body | Success |
|---|---|---|---|
| GET | `/api/v1/roles/{roleId}/events` | — | 200 `Detail[]`, ID ascending, including archived Role history |
| GET | `/api/v1/roles/{roleId}/events/{eventId}` | — | 200 `Detail` |
| POST | `/api/v1/roles/{roleId}/events` | `Create` | 201 `Detail`, Location header |
| PATCH | `/api/v1/roles/{roleId}/events/{eventId}` | `Update` | 200 `Detail` |
| POST | `/api/v1/roles/{roleId}/events/{eventId}/complete` | — | 200 `Detail` |
| POST | `/api/v1/roles/{roleId}/events/{eventId}/cancel` | — | 200 `Detail` |
| POST | `/api/v1/roles/{roleId}/events/{eventId}/participants` | `AddParticipant` | 200 `Participant` |
| DELETE | `/api/v1/roles/{roleId}/events/{eventId}/participants/{participantLinkId}` | — | 204 empty |

`Create` and `Update`: `{ "title": "팀 회고", "description": null, "startsAt": "2026-10-02T09:00:00Z", "endsAt": "2026-10-02T10:00:00Z" }`. `title` is required, trimmed, 1–120 characters. `description` is optional, trimmed, at most 1000 characters; blank becomes null. Times are optional instants; when both are set, `endsAt >= startsAt`. PATCH replaces the full structure; send existing values to retain them. Omitted optional fields become null. `AddParticipant`: `{ "participantType": "PERSON" | "SERVICE_USER", "participantId": 123 }`.

`Detail`: `{ "id", "roleId", "title", "description", "startsAt", "endsAt", "status": "PLANNED" | "COMPLETED" | "CANCELED", "completedAt", "participants": [{ "participantLinkId", "participantType", "participantId" }], "createdAt", "updatedAt", "version" }`. `completedAt` is a server-clock instant only for `COMPLETED`, otherwise null. No paging on this existing list. Successful non-204 responses use `{ "isSuccess", "code", "message", "result" }`; errors use `application/problem+json` with `status`, `code`, `title`, `detail`, and `path`.

Only an **ACTIVE** owned Role accepts commands. `PLANNED` can become `COMPLETED` or `CANCELED`; terminal events reject edits and participant changes. Repeating completion/cancellation or racing the two returns 409 `ROL-409-EVENT-NOT-PLANNED`. A lost/foreign Role or event returns 404; archived Role command returns 409 `ROL-409-ARCHIVED`; duplicate participant returns 409 `ROL-409-EVENT-PARTICIPANT-ALREADY-EXISTS`. Invalid participant identity is rejected by its provider boundary. FE may show edit and completion actions only for active Role + planned event. No LifeLog, quest, reward, or experience mutation follows these commands; a later user-authored LifeLog may link the event.

Runtime: integration commit `9599d851e89b1d0f176850b7c840470203117d43`, tree `003dc9a47750c4b6d2502147d61fbca0d11d70da`, JAR SHA-256 `087fbf94ef689cc4114c67733122e961c88304e793e08c1784aef2426f1d2f2b` on `http://127.0.0.1:19081`. CI passed for PR #389 code head `fa423f86d65bbe0f939c339813267ecbf3d4ab0f`. Real HTTP verified creation, update, participant add/remove, completion, cancellation, terminal retry, private access, no automatic LifeLog/outbox writes, and exact 13005 CORS. The PR remains Draft and unmerged.
