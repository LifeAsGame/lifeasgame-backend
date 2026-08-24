# Admin v1 API contract

This document freezes the backend-owned Admin contract inspected after PR #301. The exhaustive operation inventory is [admin-v1-endpoints.csv](admin-v1-endpoints.csv).

## Canonical boundary

- The only canonical prefix is `/admin/v1/**`.
- `SecurityConfig` protects `/admin/**` with `hasRole("ADMIN")`.
- Authentication and authority are resolved from the current persisted account on each request.
- `/api/v1/admin/**` is not a backend contract and no compatibility alias exists or may be added.

## Inventory scope

The inspection covers all 23 production `api/admin` controllers, their class and method mappings, request/response DTOs, and called Query/Service owner.

- Live mapped routes: 140
- Unmapped controller operations: 4
- Total inventoried operations: 144

| Classification | Count | Frontend rule |
| --- | ---: | --- |
| `SUPPORTED_READ` | 21 | `ALLOW` |
| `SUPPORTED_COMMAND` | 48 | `ALLOW` |
| `GATED_HIGH_RISK` | 2 | `GATE` |
| `IMPLEMENTATION_GAP` | 2 | `GATE` or `DEFER` as recorded |
| `LEGACY` | 46 | `LEGACY_ONLY` |
| `DEFERRED_PRIVATE` | 25 | `DEFER` |

## Classification definitions

- `SUPPORTED_READ`: a mapped route and current read use case with a usable response contract.
- `SUPPORTED_COMMAND`: an already mapped command supported by the current domain policy. It does not authorize inventing adjacent commands.
- `GATED_HIGH_RISK`: a command that must remain unavailable until Audit and command-hardening requirements are complete.
- `IMPLEMENTATION_GAP`: a visible controller operation that is not a proven live contract.
- `LEGACY`: Guild/Party behavior retained for compatibility only; no strategic expansion.
- `DEFERRED_PRIVATE`: LifeLog and direct Chat surfaces unavailable to general Admin UI until a privacy decision exists.

## Known User gaps

- `GET /admin/v1/users` is the only live User route.
- `AdminUserController.get` remains unmapped. Although `UserQueryService.getUserInfo(userId)` exists, `AdminUserWebMapper.toUserInfo()` currently returns placeholder null fields, so `GET /admin/v1/users/{userId}` is not safe to expose.
- `changeStatus` and `forceChangeNickname` remain unmapped and `GATED_HIGH_RISK`. Do not map them before Admin Audit plus reason/idempotency/stale-command hardening.
- `AdminUserSettingController.updateSettings` remains an unmapped private implementation gap.

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
