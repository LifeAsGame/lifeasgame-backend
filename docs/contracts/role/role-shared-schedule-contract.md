# Role shared schedule contract (3B2)

Status: **READY — API/runtime verified 2026-10-06 KST**. [Draft BE #401](https://github.com/LifeAsGame/lifeasgame-backend/pull/401) remains unmerged and depends on #396 → #397 → #398 → #400. Deployed product HEAD `58244e4337f968a4876dbaeaa990cd2edc811fe1`, tree `1ef65b28af76fc5819ad007a97e6314313ccecd3`, JAR SHA-256 `cfee483e4e9fbcae0198736b98a8d2f6fd0c868ce23bf07a60b4a5a24f7483d3` passed [full required CI](https://github.com/LifeAsGame/lifeasgame-backend/actions/runs/37336897209). This subsequent contract update does not change product source or require another app restart.

## Scope and identity

`GET /api/v1/roles/{roleId}/schedule` returns a read-only combined schedule for the authenticated Current Player's Role. `sourceType` is `ROLE_EVENT` or `GUILD_EVENT`; `(sourceType, sourceId)` is the row identity, so equal numeric IDs are distinct. A RoleEvent is shown once. A GuildEvent is shown once even if link data is duplicated. There is no event copy, synchronization, RSVP, LifeLog, Quest, reward, EXP, or notification write. Party and RoleParty have no activity source in this release and are not included. GuildGroupLink is not followed.

The existing `/api/v1/roles/{roleId}/events` array and commands, and `/api/v1/guilds/{guildId}/events` details and commands, remain unchanged. Clients open the original detail and command endpoints using `roleId/sourceId` or `guildId/sourceId`. This schedule does not grant management permission.

## Request

Query parameters: `from` and `to` are ISO-8601 timestamps with explicit offsets; both must be supplied together. `from < to`, range at most 366 days. They are converted to instants without reinterpretation in the server's default time zone. If omitted, the window is the current calendar month in `Asia/Seoul`, `[local month start, next month start)`, computed from the runtime clock. `time=DATED` (default) applies the window. `time=UNSCHEDULED` returns only RoleEvents whose start and end are both null, with its own page; `from`/`to` with `UNSCHEDULED` is 400.

`source=ALL|PERSONAL|GUILD` defaults to `ALL`. `status=PLANNED|COMPLETED|CANCELED|ALL` defaults to `PLANNED`. `participating=true` means only allowed GuildEvents with the actor's current active RSVP; it requires `source=GUILD` and `time=DATED`, otherwise 400. `time=UNSCHEDULED` with `source=GUILD` is 400. False or omitted has no participation filter. `page` is zero based, 0–1000; `size` is 1–50, default 20. Unknown enum values, invalid dates/ranges/pages, and unsupported combinations return 400. No player or owner ID is accepted.

## Time and order

Normal intervals overlap when `event.start < to && event.end > from`. RoleEvents with equal start/end, or exactly one non-null time, are point events included when `from <= point < to`. Both-null RoleEvents appear only in `UNSCHEDULED`. Stored start/end values are returned unchanged. Existing Instant columns are stored as UTC `DATETIME(6)` by the source commands; KST is used only for the default calendar month. KST midnight and month boundaries follow the supplied offsets exactly.

DATED rows sort by effective point/start instant ascending, then `sourceType` ascending, then `sourceId` ascending. UNSCHEDULED rows sort by `sourceType`, then `sourceId`; all are RoleEvents. The database applies access, filters, global order, offset and limit before returning rows. Count, rows, and RSVP use one read statement and the same live-access snapshot. A source failure fails the whole request; no partial success is reported.

## Response and access

The response uses the existing `ApiResponse` envelope and a page object `{contents, page, size, totalElements, totalPages}`. Each compact row has `sourceType`, `sourceId`, `roleId` (personal only), `guildId` and `guildName` (guild only), `title`, `startsAt`, `endsAt`, `status`, and `myRsvp` (null for personal, boolean for guild). It contains no participant or member lists, descriptions, or location. `status` is the original source's `PLANNED`, `COMPLETED`, or `CANCELED` state.

Role ownership is checked first; another player's Role returns 404. Archived Roles retain personal history. A GuildEvent appears only through that Role's direct `GUILD` personal group link while the Guild is ACTIVE and the actor is a current Guild member. Pending invitation/request, historical membership, public visibility, and an indirect GuildGroupLink grant no access. Unlinking one Role removes its Guild rows on the next read without changing another Role, the original GuildEvent, or RSVP. Leave, kick, and disband remove the Guild row and count on the next read; original GuildEvent detail/commands also recheck current membership. Original source edits and RSVP changes appear on the next read. No push or polling contract is introduced.

## Verification

Focused MySQL tests proved mixed global pagination, source ID collision, ownership and live membership, time boundaries and nullable times, filters and RSVP, and no GET mutations. The local `./gradlew clean test build` passed, followed by the full required CI above. After database/JAR/runtime backup, only the 19081 app was replaced. Flyway stayed at V47 (checksum `-699207067`); no migration was added. MySQL/Redis containers and volumes, existing 45 Players, 19080 and FE 13005 were preserved.

Actual HTTP verification with three new disposable Players passed 26 checks: mixed rows and global page, original title/time/completion updates, RSVP register/withdraw, direct link scope and unlink, membership loss hiding Guild row/count/detail, archived Role personal history, GET row/EXP invariance, previous 3A links and 3B1 Guild notes, category/catalog reads, health, and exact 13005 CORS. Original RSVP remained active after unlink and leave while current membership was absent; Player count became 48 solely through the disposable accounts. Evidence: `/Users/ryu/.local/share/lifeasgame-demo/lag-demo-129fd1f60637/feedback03b2-http-verification.json`. Credentials stay in its referenced 0600 file. Backup paths are recorded in `backend-next.json` and `preview-environment.md`.
