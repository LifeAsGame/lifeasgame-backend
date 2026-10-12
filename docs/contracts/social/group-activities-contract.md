# Party and RoleParty group activities (4A)

Status: **READY — API/runtime verified 2026-10-06 KST**. [Draft BE #402](https://github.com/LifeAsGame/lifeasgame-backend/pull/402) depends on unmerged #401 and its prior dependency chain. Product HEAD `83a380b0e508770c4898d371a5939631bab924fa`, tree `72986fbbbbaafc00eb5e9eca8dfaed10635f3e2a`, and JAR SHA-256 `9f42f1702f896e9c4c4d9cfdc40a61637f9cbb7faaf1be06b2eb1b9c6e17bbe4` passed [full required CI](https://github.com/LifeAsGame/lifeasgame-backend/actions/runs/37361204908) and run on the dedicated 19081 preview. This contract extends, and does not replace, the existing Party, RoleParty, GuildEvent, and Role schedule APIs.

## Identity and access

`groupType` is `PARTY` or `ROLE_PARTY`; `groupId` is the corresponding group's ID. An activity has one immutable owner. `(groupType, activityId)` identifies a source in the Role schedule. All routes below derive the actor Player ID from authentication. A public Party preview, Guild membership, pending invitation, former membership, and `RoleParty.roleId` alone grant no activity access.

| Current actor | Read activity and participants | RSVP self | Create/edit/complete/cancel | Manage editors |
| --- | --- | --- | --- | --- |
| Active leader of active group | Yes | Yes | Yes | Yes |
| Active member explicitly granted editor access | Yes | Yes | Yes | No |
| Other active member, including Party OFFICER | Yes | Yes | No | No |
| Pending, former, disbanded, outsider | No | No | No | No |

The former leader loses editor access at transfer unless the new leader grants it. Leave/kick/disband revoke access immediately. Rejoining never revives an old editor grant or RSVP. The grant is scoped to the current membership generation. Commands lock the owner group before checking live membership and permission; group membership changes use the same lock.

## HTTP

Base paths are `/api/v1/parties/{groupId}/activities` and `/api/v1/role-parties/{groupId}/activities`. Both expose:

| Method and suffix | Request | Success |
| --- | --- | --- |
| `GET /` | `page=0&size=20`, page 0–1000, size 1–50 | 200 page of compact activities and group capabilities |
| `POST /` | create details below | 201 activity detail, Location header |
| `GET /{activityId}` | — | 200 activity detail |
| `PATCH /{activityId}` | all details plus required `version` | 200 activity detail |
| `POST /{activityId}/complete` or `/cancel` | `{ "version": 0 }` | 200 activity detail |
| `PUT /{activityId}/rsvp` | — | 200 activity detail |
| `DELETE /{activityId}/rsvp` | — | 204 |
| `GET /{activityId}/participants` | `page=0&size=20`, size 1–50 | 200 page of `{playerId,joinedAt}` |
| `GET /editors` | `page=0&size=20`, size 1–50 | 200 page of `{playerId}`; leader only |
| `PUT /editors/{playerId}` | — | 204; leader only |
| `DELETE /editors/{playerId}` | — | 204; leader only |

Create example:

```json
{"clientRequestId":"7b8b6f75-84a7-4ecd-bb1e-17a1611e8cb4","title":"Saturday hike","sharedDescription":"Meet at the east gate","location":"East gate","startsAt":"2026-10-10T09:00:00+09:00","endsAt":"2026-10-10T12:00:00+09:00"}
```

`clientRequestId` is a required UUID. Repeating the same group, actor, key, and details returns the original activity without another mutation; changing details under the same key returns 409. Each group has its own key namespace. `PATCH` is a full replacement of the editable fields: `title`, `sharedDescription`, `location`, `startsAt`, `endsAt`, and `version` are required as applicable. Nullable description/location may be omitted or null to clear. No group move is supported. A detail row contains `id`, `groupType`, `groupId`, editable fields, `status`, `createdByPlayerId`, `createdAt`, `updatedAt`, `version`, `participantCount`, `myRsvp`, and `capabilities` (`canEdit`, `canManageEditors`, `canRsvp`). Activity list rows have the same row shape, without any member or participant array. The activity list page is `{contents,page,size,totalElements,totalPages,capabilities:{canCreate,canManageEditors}}`, including when `contents` is empty. Participant and editor pages use `{contents,page,size,totalElements,totalPages}`. All are in the standard `ApiResponse` envelope. These capabilities guide UI display; every command rechecks live permission.

Title is trimmed, 1–120 characters; description is optional up to 2000; location is optional up to 200. Both times are required ISO offset date-times (`Z` accepted), stored as instants, within the existing MySQL `DATETIME` boundary after conversion to `Asia/Seoul`; end must be strictly after start. Status starts `PLANNED` and may change once to `COMPLETED` or `CANCELED`. Only planned activities can be edited or RSVP changed. A stale `version` yields 409 and never overwrites newer data. Missing or malformed required fields/version yield 400. Repeated PUT/DELETE RSVP converge without duplicate participants; repeated finish with the current terminal version succeeds only for the same terminal action, while a stale version or opposite terminal action yields 409. Participant rows and count include only active RSVP from currently active membership in the same generation. Activity and RSVP history are retained after finish or membership loss. No automatic RoleEvent, LifeLog, Quest, EXP, reward, or notification is created.

The standard problem response uses `application/problem+json`. Missing or inaccessible group/activity returns 404; invalid fields and query parameters return 400; stale version, terminal-state mutation, and reused request key with different content return 409. A member lacking edit permission receives 403; editor management is leader only. Network retry of create must reuse the same `clientRequestId`; retry of versioned commands should first read current detail after a 409.

## Personal Role schedule extension

`GET /api/v1/roles/{roleId}/schedule` retains existing `PERSONAL`, `GUILD`, `ALL`, `DATED`, `UNSCHEDULED`, status, and page semantics. Add `source=PARTY|ROLE_PARTY|SHARED`. `ALL` includes all four source types; `SHARED` includes Guild, Party, and RoleParty. `participating=true` is valid with `GUILD`, `PARTY`, `ROLE_PARTY`, or `SHARED`, only with `DATED`, and filters to the actor's current RSVP. `PERSONAL` and `UNSCHEDULED` remain incompatible with it. `UNSCHEDULED` still contains personal RoleEvents only.

Party/RoleParty rows require a direct personal group link for that owned Role, an ACTIVE source group, and current source membership. Source types are `PARTY_ACTIVITY` and `ROLE_PARTY_ACTIVITY`. Each row adds `groupType`, `groupId`, and `groupName`; `guildId` and `guildName` remain for Guild rows. Source identity is `(sourceType,sourceId)`. The existing database query performs access, overlap (`startsAt < to && endsAt > from`), status and RSVP filters, global ordering `(startsAt,sourceType,sourceId)`, count, offset and limit in one statement/snapshot. A RoleParty's creator `roleId` is only provenance; it grants no personal link or shared access. Creating a RoleParty and then linking it are separate requests: preserve the creation ID and retry only the failed link. Unlink hides rows from that Role only and does not alter the activity or RSVP.

## Verification and handoff

Flyway V48 applied on 19081 with checksum `-1929471430`; Hibernate validation and health passed. The database, prior JAR, and prior runtime settings were backed up before V48 at `/Users/ryu/.local/share/lifeasgame-demo/lag-demo-129fd1f60637/backups/feedback04a-before-v48-20261006.sql.gz`, `/Users/ryu/.local/share/lifeasgame-demo/lag-demo-129fd1f60637/app.jar.pre-feedback04a-20261006`, and `/Users/ryu/.local/share/lifeasgame-demo/lag-demo-129fd1f60637/runtime.json.pre-feedback04a-20261006`. Only the app JAR was replaced; MySQL/Redis containers and volumes, 19080, and FE 13005 remained in place.

Actual HTTP verification with three disposable Players passed 46 checks: Party/RoleParty create, editor grant/revoke, member edit, retry and version conflicts, RSVP and participant counts, Role links and global paging, participation filtering, completion, unlink and departure privacy, empty-list create capabilities, no LifeLog/Quest/reward/outbox/EXP side effects, prior 3B2/3B1/category/catalog reads, health, and exact 13005 CORS. The original 48 Players were preserved; the three verification Players brought the count to 51. Evidence: `/Users/ryu/.local/share/lifeasgame-demo/lag-demo-129fd1f60637/feedback04a-http-verification.json`. Credentials remain only in its referenced 0600 file. FE #157 live two-account verification remains FE-owned; this BE contract is ready for it.
