# Social management contract

All routes below require the signed-in service user's current Player. The server derives `playerId`; FE never supplies it except target IDs on existing management commands. `Guild`, ordinary `Party`, and `RoleParty` are distinct.

## Creation and discovery

Both `/api/v1/guilds` and `/api/v1/parties` share these read paths and request semantics. Responses use the standard `ApiResponse.result` envelope.

| Method/path under each base | Data / access |
| --- | --- |
| `POST /` | Existing `Info`. Required `name`, `code`, `visibility`, `joinPolicy`, `maxMembers`; optional description and emblem (Guild) or banner (Party). |
| `GET /search?keyword=&visibility=&page=0&size=20` | `Page<Summary>` of active PUBLIC groups only; `visibility=PRIVATE` yields an empty page. `page>=0`, `1<=size<=100`. |
| `GET /recent?limit=20` | Up to 100 newest active PUBLIC `Summary` entries. |
| `GET /{id}/preview` | PUBLIC active `Summary` for joining decisions; private outsiders receive 404. |
| `GET /mine?page=0&size=20` | `Page<MyGuild>` or `Page<MyParty>` for current memberships, including PRIVATE. |
| `GET /{id}` | Existing `Info` for current members only; a nonmember receives 404. |
| `GET /{id}/members?page=0&size=20` | `Page<Member>` for current members only. |
| `GET /{id}/me` | Current player's role, pending join/invite and allowed actions. PUBLIC outsiders may query; PRIVATE outsiders receive 404 unless they have an invitation. |
| `GET /requests?page=0&size=20` | Current player's pending join requests only. |
| `GET /invitations?page=0&size=20` | Current player's pending invitations, with only group ID/name/code and invitation details needed to accept or decline. |
| `GET /{id}/pending-requests?page=0&size=20` | Pending join requests for current leader only. |

`Summary` is `{id,name,code,visibility,joinPolicy,status,maxMembers}`. `Info` is the existing group detail DTO. `MyGuild`/`MyParty` is `{id,name,code,status,myRole,memberCount,maxMembers}`. `Member` is `{playerId,role,joinedAt}`; the existing `WaitMember` DTO carries `{id,guildId|partyId,name,code,playerId,type,status,message,requestedAt,expiresAt}` on pending lists. `Page<T>` is `{contents,page,size,totalElements,totalPages}`. `Me` includes `myRole` (nullable), `pendingJoin`, `pendingInvitation`, and `actions` (strings matching existing command path suffixes). Only active groups are discoverable.

`visibility` is `PUBLIC` or `PRIVATE`. `joinPolicy` is `OPEN`, `APPROVAL`, or `INVITE_ONLY`. `OPEN` joins immediately through `request-join`; `APPROVAL` leaves a pending request; `INVITE_ONLY` requires invitation. `maxMembers` is an explicit integer from 1 to 500, not a hidden default. `code` is required and nonblank. PUBLIC describes discovery, not unrestricted detail or membership. A PRIVATE group is omitted from global search, recent, counts and pages regardless of keyword. A current member can see it in `/mine`; an invitee gets only the invitation summary. A pending applicant sees only their own request. Private direct preview remains hidden to outsiders.

An expired invitation can remain in `/invitations` while its stored status is `PENDING`. Its `expiresAt` remains visible; `/me` reports `pendingInvitation=true` but omits `accept-invitation` from `actions` and still permits `decline-invitation`.

## Existing commands and permissions

Existing command routes remain `POST /{id}/{operation}`. `request-join`, `cancel-join`, `accept-invitation`, `decline-invitation`, `leave` are current-player actions. `approve`, `reject`, `transfer-leader`, `promote`, `demote`, `disband` require LEADER. `invite` and `kick` require LEADER or OFFICER. `rename`, `policy`, `description`, emblem/banner and tag edits retain the existing owner/leader restriction; leadership transfer changes the authoritative leader and owner player ID. Target player IDs in command bodies identify the target, never the actor. `Me.actions` reflects these existing rights; it is UI guidance, and commands enforce their own rights.

Unauthorized private group reads and missing IDs use the existing `SOC-404-GUILD-NOT-FOUND` / `SOC-404-PARTY-NOT-FOUND` response. Manager-only request lists use `SOC-403-NOT-MEMBER` or `SOC-403-LEADER-ONLY`. Invalid pagination and enum input are 400. Normal authentication errors retain the platform contract. FE should use the returned role, policy, capacity and actions rather than assuming public/free joining or a 20-member limit.

| Actor | PUBLIC preview | PRIVATE detail | Members | Own pending/invite | Group pending requests | Commands |
| --- | --- | --- | --- | --- | --- | --- |
| Outsider | Yes | 404 | 404 | Only own records | 403 | Join only when public and policy permits |
| Invitee | Yes if public | 404 until joined | 404 | Own invitation summary | 403 | Accept/decline own invitation |
| Member | Yes if public | Yes | Yes | Only own records | 403 | Leave; officer additionally invites/kicks |
| Leader | Yes if public | Yes | Yes | Only own records | Yes | Existing leader commands and edits |

| Error condition | HTTP / code |
| --- | --- |
| Missing or hidden group detail/preview | 404 / `SOC-404-GUILD-NOT-FOUND` or `SOC-404-PARTY-NOT-FOUND` |
| Nonmember on a manager queue | 403 / `SOC-403-NOT-MEMBER` |
| Member without leader rights on a manager queue | 403 / `SOC-403-LEADER-ONLY` |
| Invalid page, size, visibility or creation input | 400 / existing validation or Social error code |
| No authentication | 401 / platform authentication response |

## Related contracts

- [RoleParty membership and invitations](role-party-contract.md)
- [Guild group links and shared events](guild-groups-events-contract.md)
- [Group activities](group-activities-contract.md)
- [Guild group creation and offline roster](guild-create-roster-contract.md)

These contracts define the implemented boundaries. Group membership never grants access to personal Role notes or Person records.
