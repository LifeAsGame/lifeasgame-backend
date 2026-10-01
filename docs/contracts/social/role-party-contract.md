# RoleParty invitation-only group contract

Status: **leader invitation listing implemented; runtime readiness tracked in the shared integration handoff**. Target base: `origin/develop` `7520f3a99c3e234116f242d2f299a6d884811f9c`. Previous implementation SHA: `4f86c80f4fa9e5b2a532ab99d9d4ca9eef830916`. `RoleParty` is separate from ordinary Party and Guild. Creation needs an active owned Role, and needs no RoleEvent. A RoleParty member gains no access to that personal Role, Person, or private events.

Every route requires the current authenticated **Player**; the server derives the actor Player ID. `roleId` is the creator's private Role ID. `inviteePlayerId` and `toPlayerId` are **Player IDs**, not User or Person IDs; invite targets must be mutual active follows (visible across `/api/v1/connections/followings` and `/followers`). The form should say **invitation-only**. No public search or open join exists.

| Method | Path | Body | Success |
|---|---|---|---|
| GET/POST | `/api/v1/roles/{roleId}/role-parties` | POST `{ "name", "description", "maxMembers" }` | GET 200 paged summaries; POST 201 member detail |
| GET | `/api/v1/role-parties/mine` | — | 200 paged `{ "group": Summary, "membershipStatus": "ACTIVE" | "LEFT" }` for current/former members |
| GET | `/api/v1/role-parties/invitations/mine` | — | 200 paged minimal pending invitations |
| GET | `/api/v1/role-parties/{id}/invitations` | — | 200 paged unexpired pending invitations, current leader only |
| GET | `/api/v1/role-parties/{id}` | — | 200 member detail |
| GET | `/api/v1/role-parties/{id}/members` | — | 200 paged member summaries |
| PATCH | `/api/v1/role-parties/{id}` | `{ "name", "description", "maxMembers" }` | 200 member detail, leader only |
| POST | `/api/v1/role-parties/{id}/invitations` | `{ "inviteePlayerId" }` | 200 invitation, leader only |
| POST | `/api/v1/role-parties/{id}/invitations/{invitationId}/accept` or `/decline` | — | 200 member detail or 204, invitee only |
| DELETE | `/api/v1/role-parties/{id}/invitations/{invitationId}` | — | 204, leader only |
| POST | `/api/v1/role-parties/{id}/leave` | — | 204, nonleader member only |
| POST | `/api/v1/role-parties/{id}/transfer-leader` | `{ "toPlayerId" }` | 200 member detail, leader only |
| POST | `/api/v1/role-parties/{id}/disband` | — | 200 member detail, leader only |

Collections accept `page=0&size=20` by default, with size 1–100, and return `{ "contents": [...], "page", "size", "totalElements", "totalPages" }` inside the standard `{ "isSuccess", "code", "message", "result" }` envelope. Errors use `application/problem+json` with `status`, `code`, `title`, `detail`, and `path`. `name` is 1–120 trimmed characters, `description` nullable up to 1000, `maxMembers` is required and 2–50 including the leader. PATCH sends the whole structure. Shared summary/detail includes only `id`, `name`, `description`, `status` (`ACTIVE`/`DISBANDED`), `creatorPlayerId`, `leaderPlayerId`, `memberCount`, `maxMembers`, dates, and active member `{ "playerId", "role": "LEADER" | "MEMBER", "joinedAt" }` rows; no Role ID/title/notes, Person, or RoleEvent data. The Role list path itself supplies the `roleId` to its owner. Invitation shows `invitationId`, `rolePartyId`, `groupName`, `inviterPlayerId`, `inviteePlayerId`, `expiresAt`, and `status`, sufficient to accept/decline.

Creator starts as `LEADER` and first member. A friend joins only after accepting an unexpired invitation; pending invitations expire after seven days. Pending resend returns the same invitation; accepted repeat returns the existing membership without duplication; a former member can rejoin after a new invitation; other invalid transitions and full capacity return 409. Last-seat acceptance is serialized at the database group row. `ACTIVE` leader can update, invite, cancel invitations, transfer leadership, and disband; an `ACTIVE` nonleader member can view and leave; an invitee can view only their invitation and accept or decline. Leader must transfer to another active member or disband before leaving. Archiving the source Role blocks new groups but leaves existing groups active. Disband blocks further edits/invites/acceptance and retains member history. Outsiders and former members cannot open detail and receive 404; an invitee sees only their own minimal invitation. Missing/foreign group and invitation return 404 `SOC-404-ROLE-PARTY-*`; invalid input returns 400, full group/expired invitation/state conflict returns 409. No Guild link, event link, chat, or automatic LifeLog/reward is part of this contract.

Leader invitation listing returns only `PENDING` rows whose `expiresAt` is strictly after server time, newest first. GET never changes invitation state. The current active leader can list and cancel remaining invitations after transfer; outsiders, ordinary or former members, and the previous leader receive 404. Accepted, declined, canceled, and expired rows never appear as cancelable items. The invitee-only `/invitations/mine` contract is unchanged.

Runtime (previous deployment, before leader listing): integration commit `9599d851e89b1d0f176850b7c840470203117d43`, tree `003dc9a47750c4b6d2502147d61fbca0d11d70da`, JAR SHA-256 `087fbf94ef689cc4114c67733122e961c88304e793e08c1784aef2426f1d2f2b` on `http://127.0.0.1:19081`; V40 is applied while V38/V39 checksums and existing data remain. CI passed for PR #390 code head `8287ee4344e0b6be89d19c665d19c32d5e2fa2f4`. Real HTTP verified group creation before any event, invite/resend/accept, member privacy, leader transfer, leave, Role archive, disband, unchanged legacy reads, and exact 13005 CORS. The PR remains Draft and unmerged.
