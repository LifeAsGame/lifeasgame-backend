# Admin v1 API contract

This document freezes the backend-owned Admin contract inspected after PR #301. The exhaustive operation inventory is [admin-v1-endpoints.csv](admin-v1-endpoints.csv).

## Canonical boundary

- The only canonical prefix is `/admin/v1/**`.
- `SecurityConfig` protects `/admin/**` with `hasRole("ADMIN")`.
- Authentication and authority are resolved from the current persisted account on each request.
- `/api/v1/admin/**` is not a backend contract and no compatibility alias exists or may be added.

## Inventory scope

The inspection covers all 24 production Admin controllers, their class and method mappings, request/response DTOs, and called Query/Service owner.

- Live mapped routes: 149
- Unmapped controller operations: 4
- Total inventoried operations: 153

| Classification | Count | Frontend rule |
| --- | ---: | --- |
| `SUPPORTED_READ` | 30 | `ALLOW` |
| `SUPPORTED_COMMAND` | 17 | `ALLOW` |
| `GATED_HIGH_RISK` | 33 | `GATE` |
| `IMPLEMENTATION_GAP` | 2 | `GATE` or `DEFER` as recorded |
| `LEGACY` | 46 | `LEGACY_ONLY` |
| `DEFERRED_PRIVATE` | 25 | `DEFER` |

## Classification definitions

- `SUPPORTED_READ`: a mapped route and current read use case with a usable response contract.
- `SUPPORTED_COMMAND`: a mapped low-risk command currently approved for operator exposure. It does not authorize inventing adjacent commands or imply that every mapped command is safe for frontend exposure.
- `GATED_HIGH_RISK`: a mapped or implemented command that must remain unavailable to the frontend until durable Admin Audit and command-hardening requirements are complete.
- `IMPLEMENTATION_GAP`: a visible controller operation that is not a proven live contract.
- `LEGACY`: Guild/Party behavior retained for compatibility only; no strategic expansion.
- `DEFERRED_PRIVATE`: LifeLog and direct Chat surfaces unavailable to general Admin UI until a privacy decision exists.

Mapping existence and frontend capability are separate. `SUPPORTED_COMMAND + ALLOW` means both mapped and currently approved; a mapped high-risk command remains `GATED_HIGH_RISK + GATE`.

## Command risk decision

All 50 mapped command rows in the supported/gated command classifications were reviewed. Twelve non-destructive definition/catalog authoring commands remain `ALLOW`: create/update for Achievement, Certification, Hobby, Title, and Item, plus Quest definition ensure/update. The Wallet adjustment command, two Quest Acceptance override commands, and Inventory/Mailbox entitlement commands are also `ALLOW` after their Unit E hardening. Their exact contracts are [admin-wallet-adjustment-contract.md](admin-wallet-adjustment-contract.md), [admin-quest-acceptance-contract.md](admin-quest-acceptance-contract.md), and [admin-inventory-entitlement-contract.md](admin-inventory-entitlement-contract.md).

The other 33 mapped high-risk commands remain `GATE`: destructive catalog deletion, direct player-state correction, other entitlement grant/revoke operations, live Shop/economy mutation, and social relationship mutation. Their routes remain mapped but are not frontend capabilities before durable Admin Audit and reason/idempotency/stale/conflict hardening.

## Inventory inspection reads

The four bounded Item, Inventory, and Mailbox reads are defined in
[admin-inventory-inspection-contract.md](admin-inventory-inspection-contract.md).
They reuse Inventory-owned QueryServices and expose no per-instance attributes.

## Player holder inspection reads

The four bounded Achievement, Certification, Hobby, and Title holder reads are
defined in
[admin-player-holder-inspection-contract.md](admin-player-holder-inspection-contract.md).
They use the requested path `playerId`; free-form holder content and definition
bodies are excluded.

## Known User gaps

- `GET /admin/v1/users` is the only live User route.
- `AdminUserController.get` remains unmapped. Although `UserQueryService.getUserInfo(userId)` exists, `AdminUserWebMapper.toUserInfo()` currently returns placeholder null fields, so `GET /admin/v1/users/{userId}` is not safe to expose.
- `changeStatus` and `forceChangeNickname` remain unmapped and `GATED_HIGH_RISK`. Do not map them before Admin Audit plus reason/idempotency/stale-command hardening.
- `AdminUserSettingController.updateSettings` remains an unmapped private implementation gap.

## Admin Audit read

- `GET /admin/v1/audit-events` is the canonical safe metadata reader and is
  `SUPPORTED_READ + ALLOW`.
- Its durable append and transaction rules are defined in
  [admin-audit-contract.md](admin-audit-contract.md).
- Except for the hardened Wallet adjustment, Quest Acceptance override, and
  Inventory/Mailbox entitlement commands, existing high-risk commands remain
  `GATED_HIGH_RISK + GATE` until their own Unit E integration.

## Frontend migration rules

1. Do not treat frontend `/api/v1/admin/**` paths as canonical.
2. Connect adapters only to CSV rows marked `ALLOW`.
3. Do not invent a backend endpoint for a frontend-only or unproven action.
4. Do not expose `GATE` or `DEFER` rows as UI capabilities.
5. Resolve method and DTO drift in favor of this backend contract.
6. Do not add backend aliases to preserve mock-era frontend paths.
7. Use Guild/Party routes for legacy maintenance only and do not expand them.

## Change rule

The CSV reflects current backend mappings; it is not permission to broaden Admin behavior. Any future route or classification change requires its own backend review and focused contract update.
