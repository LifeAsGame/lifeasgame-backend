# Social management contract (SOCIAL-01)

Status: implementation contract, 2026-10-01. All routes below require the signed-in service user's current Player. The server derives `playerId`; FE never supplies it except target IDs on existing management commands. `Guild`, ordinary `Party`, and future `RoleParty` are distinct.

## Creation and discovery

Both `/api/v1/guilds` and `/api/v1/parties` share these read paths and request semantics. Responses use the standard `ApiResponse.data` envelope.

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

`Summary` is `{id,name,code,visibility,joinPolicy,status,maxMembers}`. `Info` is the existing group detail DTO. `MyGuild`/`MyParty` is `{id,name,code,status,myRole,memberCount,maxMembers}`. `Member` is `{playerId,role,joinedAt}`; pending entries carry `{id,guildId|partyId,name,code,playerId,type,status,message,requestedAt,expiresAt}`. `Page<T>` is `{contents,page,size,totalElements,totalPages}`. `Me` includes `myRole` (nullable), `pendingJoin`, `pendingInvitation`, and `actions` (strings matching existing command path suffixes). Only active groups are discoverable.

`visibility` is `PUBLIC` or `PRIVATE`. `joinPolicy` is `OPEN`, `APPROVAL`, or `INVITE_ONLY`. `OPEN` joins immediately through `request-join`; `APPROVAL` leaves a pending request; `INVITE_ONLY` requires invitation. `maxMembers` is an explicit integer from 1 to 500, not a hidden default. `code` is required and nonblank. PUBLIC describes discovery, not unrestricted detail or membership. A PRIVATE group is omitted from global search, recent, counts and pages regardless of keyword. A current member can see it in `/mine`; an invitee gets only the invitation summary. A pending applicant sees only their own request. Private direct preview remains hidden to outsiders.

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

## Next implementation: SOCIAL-02 proposal (not implemented here)

| Existing model | Identity and link | Proposed role |
| --- | --- | --- |
| Role | Personal Player-owned Role ID | Private organizing context; does not itself confer membership. |
| RoleEvent | Personal Role-owned event ID | Optional event link; cancelling an event does not create or remove a LifeLog. |
| Person | Player-owned record of another person | May annotate participation; never substitutes for an authenticated service user. |
| Guild | Service Player membership and leader | Persistent shared organization. |
| Party | Service Player membership and leader | Ordinary group; can exist without Role or Guild. |
| RoleParty | New shared group ID with creator Player, Role ID association and service Player memberships | Small group in a Role, independent of legacy Party to preserve existing Party permissions and lifecycle. |

Choose an independent `RoleParty` aggregate, with `role_party_id`, creator/current leader player ID, owning Role ID, optional `guild_id`, optional `role_event_id`, status, visibility, join policy, capacity, and explicit service Player membership/invitation rows. This retains existing ordinary Party permissions and lifecycle without reinterpretation. Role owner must explicitly create or link it; Role ownership does not automatically grant shared rights, and linked guild membership does not automatically join the RoleParty. Linking a guild requires a current Guild leader's approval and provider-owned Guild application API. Keep only IDs across contexts; do not import foreign Entity/Repository. A Role's personal notes and Person records remain private unless an explicit publication contract is later approved.

For Guild internal navigation, add an explicit `guild_party_links(guild_id, party_id, linked_by_player_id, status, linked_at)` association for ordinary Parties, with unique active `(guild_id, party_id)` and leader approval at both boundaries. The Party remains usable after unlinking and still needs no Role. Shared Guild events need their own Guild-owned event contract or an explicitly published event snapshot; do not point Guild members at a private `RoleEvent` by ID alone. Proposed `guild_event_links` should record the published event ID, Guild ID, publisher, publication state, and visibility after that event contract is approved. These links are SOCIAL-02 schema proposals only.

Proposed routes: `GET /api/v1/roles/{roleId}/role-parties`, `POST /api/v1/roles/{roleId}/role-parties`, `GET /api/v1/role-parties/{id}`, and scoped participant/invitation management; `GET /api/v1/guilds/{guildId}/role-parties`, `/parties`, and `/events` for a Guild member; and optional `POST /api/v1/role-parties/{id}/event-link`. Role list access follows Role ownership; shared details follow RoleParty membership; Guild navigation follows Guild membership; invitees get a minimal invitation only. Group creation must work before any event exists. An authenticated user is a participant only after joining; a Person mentioned in a Role/Event is merely a personal record.

On Guild leave, revoke Guild-derived navigation and membership where explicitly bound, preserving historical participation records. On RoleParty closure, stop joining/commands, retain an auditable closed summary for former members. On Role archive, prevent new RoleParty creation while existing shared groups retain their own lifecycle and a safe historical link. On event cancellation, clear future event navigation but retain RoleParty and event history. Acceptance: Role → RoleParty list → create within Role → participant/detail flow; Guild → members/groups/events navigation; ordinary Party still works without Role; no personal note/Person disclosure; cross-context permissions and lifecycle covered by focused tests. Confirm these choices in the SOCIAL-02 request before adding schema or API.
