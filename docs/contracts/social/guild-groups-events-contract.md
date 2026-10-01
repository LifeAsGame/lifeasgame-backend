# Guild groups and shared events — FE contract

Status: **DRAFT — API design shared before implementation**. Runtime 19081 is **not ready for these endpoints**. Source base: `develop` `6adec4daae47633a7e58fb3a7801a292892b9c15` (tree `ee7e9c1ef278b7759a6cc1e9c8a4d5363e38821e`).

All routes require an authenticated Current Player. The server derives the Player ID from authentication. Success uses `{ "isSuccess": true, "code": "COMMON-200" | "COMMON-201", "message": "...", "result": ... }`; 204 has no body. Errors use `application/problem+json` with `status`, `code`, `title`, `detail`, `path`. Collections use `page=0&size=20` (size 1–100) and `{ "contents", "page", "size", "totalElements", "totalPages" }`. Times are ISO-8601 instants with offsets in requests and UTC instants in responses. `location` is nullable; omitted and null both clear it on full PATCH.

## Linked groups

`groupType` is exactly `PARTY` or `ROLE_PARTY`. This links an existing group; it never changes its owner, membership, visibility, or source Role. A Guild member is not thereby a group member. `displayName` is a separate, explicit Guild-facing label (1–120 trimmed characters); neither group name nor description is copied into it.

| Method | Path | Body | Success / access |
| --- | --- | --- | --- |
| GET | `/api/v1/guilds/{guildId}/group-links` | — | 200 page of **ACTIVE** links; active Guild members only |
| GET | `/api/v1/guilds/{guildId}/group-links/pending` | — | 200 page of pending links relevant to the current Guild leader, current group leader, or proposer only |
| POST | `/api/v1/guilds/{guildId}/group-links` | `{ "groupType", "groupId", "displayName" }` | 201 link; one current leader proposes; both leaders in one Player activates immediately |
| POST | `/api/v1/guilds/{guildId}/group-links/{linkId}/approve` | `{ "displayName" }` | 200 link; other current leader explicitly confirms final label; current authority rechecked |
| POST | `/api/v1/guilds/{guildId}/group-links/{linkId}/reject` | — | 200 terminal link; either current leader |
| POST | `/api/v1/guilds/{guildId}/group-links/{linkId}/cancel` | — | 200 terminal link; proposer |
| DELETE | `/api/v1/guilds/{guildId}/group-links/{linkId}` | — | 204; either current leader; source group and memberships remain |

Active list item: `{ "id", "groupType", "groupId", "displayName", "status": "ACTIVE", "entryAction" }`. `entryAction` is `OPEN_DETAIL` for a current source-group member, `OPEN_PUBLIC_PREVIEW` for an eligible public Party, otherwise `INVITE_REQUIRED`. Pending/command result additionally contains `{ "proposedByPlayerId", "guildLeaderApproved", "groupLeaderApproved", "createdAt", "updatedAt" }`; statuses are `PENDING`, `ACTIVE`, `REJECTED`, `CANCELED`, `UNLINKED`. Approval flags describe **current** leaders, so a leader change may make an old approval insufficient. Private group name, description, members, personal Role/Person IDs, and schedules are never projected in these link responses. Source group detail still requires its own membership; public Party preview follows its existing policy.

One open (`PENDING` or `ACTIVE`) link per `(guildId, groupType, groupId)` is enforced in MySQL. `guildId` has a real FK. The two allowlisted source tables cannot share one `groupId` FK; the application verifies existence, active status, and the current leader while locking the source row. Source groups have no hard-delete player API. Retry of the same proposal returns the existing open link if authorized; approval/unlink retry returns 409 without duplicate mutation. Missing/foreign links and unauthorized reads return 404 `SOC-404-GUILD-GROUP-NOT-FOUND`; missing Guild is `SOC-404-GUILD-NOT-FOUND`. Invalid type/label/page is 400 `SOC-400-GUILD-GROUP-INVALID-INPUT`. Stale link state or disbanded Guild/group is 409 `SOC-409-GUILD-GROUP-CONFLICT`. Guild departure immediately removes access to the active list while separate group membership remains.

## Guild shared events

GuildEvent is separate from personal RoleEvent. No personal event, Person participant, LifeLog, quest, reward, XP, or message is created or copied by these endpoints.

| Method | Path | Body | Success / access |
| --- | --- | --- | --- |
| GET | `/api/v1/guilds/{guildId}/events` | — | 200 page of event summaries; active Guild members |
| POST | `/api/v1/guilds/{guildId}/events` | `{ "title", "sharedDescription", "startsAt", "endsAt", "location" }` | 201 detail; current Guild leader |
| GET | `/api/v1/guilds/{guildId}/events/{eventId}` | — | 200 detail; active Guild members |
| PATCH | `/api/v1/guilds/{guildId}/events/{eventId}` | same full body as POST | 200 detail; current Guild leader; `PLANNED` only |
| POST | `/api/v1/guilds/{guildId}/events/{eventId}/complete` | — | 200 detail; current Guild leader |
| POST | `/api/v1/guilds/{guildId}/events/{eventId}/cancel` | — | 200 detail; current Guild leader |
| PUT | `/api/v1/guilds/{guildId}/events/{eventId}/rsvp` | — | 200 detail; own participation; `PLANNED` only |
| DELETE | `/api/v1/guilds/{guildId}/events/{eventId}/rsvp` | — | 204; own participation; `PLANNED` only |
| GET | `/api/v1/guilds/{guildId}/events/{eventId}/participants` | — | 200 page of current Guild members with RSVP, each `{ "playerId", "joinedAt" }` |

Summary/detail: `{ "id", "guildId", "title", "sharedDescription", "startsAt", "endsAt", "location", "status", "createdByPlayerId", "participantCount", "myRsvp", "createdAt", "updatedAt" }`. `status` is `PLANNED`, `COMPLETED`, or `CANCELED`. `myRsvp` is a boolean derived from persisted RSVP, so refresh restores it. `participantCount` excludes people who have left the Guild; their historical RSVP row remains. Title 1–120, shared description 0–2000, location null or 1–200, and `endsAt` must be after `startsAt`. The server locks the event for transitions, uses server time, and rejects terminal changes with 409. PUT RSVP is idempotent; DELETE absent RSVP is idempotent. Leaving or disbanding removes access and blocks new commands; a later rejoin does not erase the historical RSVP.

Capabilities: only active Guild members see the event panel. Current leader sees create/edit/complete/cancel controls; other members see only their own RSVP control. The server rechecks authority on every command after leader transfer. FE must not infer Guild event access from Party/RoleParty membership or use a link as a detail access token.

Event errors: missing/foreign event is 404 `SOC-404-GUILD-EVENT-NOT-FOUND`; nonmember or disbanded Guild is 404 `SOC-404-GUILD-NOT-FOUND`; a member without leader authority receives 403 `SOC-403-LEADER-ONLY`. Invalid title/time/location/page is 400 `SOC-400-GUILD-EVENT-INVALID-INPUT`; any change after `COMPLETED` or `CANCELED` is 409 `SOC-409-GUILD-EVENT-CONFLICT`.
