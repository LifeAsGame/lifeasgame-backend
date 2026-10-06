# Achievement and title activation — DRAFT

This contract records the 2026-10-06 activation decision. Earlier catalog rows were GATED, DEFERRED or candidates; they did not authorize automatic grants. Implementation, definition data and 19081 runtime readiness are tracked separately below.

| Readiness | State |
| --- | --- |
| Implementation | LOCAL TESTED; required CI pending |
| Definition data | V50 local MySQL tested; 19081 pending |
| Dedicated 19081 runtime | NOT VERIFIED |

## Activated definitions and conditions

| Stable code | Korean name | Server condition |
| --- | --- | --- |
| `ACH_FIRST_LIFELOG` | 첫 기록 | First committed, content-ready, user-authored `LifeLogRecorded` for the Player. Valid existing record subtypes include QUICK_NOTE, ACTIVITY, STUDY, PROJECT, MEMORY, REFLECTION, MOOD and HEALTH_NOTE. System-generated and incomplete legacy entries do not qualify. Quest acceptance is irrelevant. |
| `ACH_FIRST_QUEST_COMPLETE` | 첫 퀘스트 완료 | First server-confirmed `COMPLETED` QuestAcceptance. GOAL_REACHED and reward settlement alone do not qualify. |
| `ACH_FIRST_ITEM_CLAIM` | 첫 아이템 수령 | First committed Mailbox Claim or ClaimAll that moves an item into Inventory. Mail arrival, Inventory possession and market purchase do not qualify. |
| `ACH_ROUTE_RECORD_START` | 기록 여정 완주 | `ROUTE_RECORD_START` actually becomes COMPLETED on the last explicit advance. |
| `ACH_ROUTE_BACKEND_START` | 백엔드 개발자 여정 완주 | `ROUTE_BACKEND_DEVELOPER_START` actually becomes COMPLETED on the last explicit advance. |
| `TITLE_CANDIDATE_RECORD_BEGINNER` | 기록의 시작 | Granted with a justified `ACH_FIRST_LIFELOG` acquisition. |
| `TITLE_BACKEND_GUIDE` | 백엔드 길잡이 | Granted with a justified `ACH_ROUTE_BACKEND_START` acquisition. |

These are the only newly activated definitions. Stable codes, including `CANDIDATE`, remain unchanged. Existing persisted Korean names take precedence if the same code already exists; any mismatch must be reported before data changes. All seven definitions are versioned. Awards are once per Player and definition, independent of later definition revisions or event redelivery. Reward profile is RP_NONE: no additional EXP, gold, item or stat grant. Titles use PlayerTitle ownership and optional Player representative title; they are not Inventory items or equipment. Neither title is automatically selected. The backend journey remains a self-recorded activity, not external certification or deployment validation.

## Player API and FE behavior

The existing achievement/title catalog, owned achievement/title lists and `PATCH /api/v1/players/titles/{titleId}` remain compatible. `GET /api/v1/players/activated-content?page=0&size=7` exposes only the five activated achievements and two linked titles in the table order above. It returns `ApiResponse.result` with `entries[]`, `page`, `size`, `hasNext`. Each entry has `kind` (`ACHIEVEMENT`/`TITLE`), stable `code`, `definitionId`, `name`, `definitionVersion`, Korean `condition`, `status` (`UNACQUIRED`/`ACQUIRED`), `evidenceStatus` (`NONE`/`CONFIRMED`/`ADMIN_OR_LEGACY`/`REVOKED`), `acquiredAt`, and `sourceOccurredAt`. Size is 1–20; page is zero-based. `acquiredAt` is the actual grant time; `sourceOccurredAt` is the qualifying fact time when known. They must not be conflated during reconciliation. Existing recent-achievement read API is not a percentage-progress API. A narrow `DELETE /api/v1/players/titles/representative` returns `UpdatedTitle.titleId=null` and clears the nullable representative title. Selecting another owned title replaces the selection; a foreign or unowned title is rejected. Granting a title does not change the selection. Revocation of the selected title clears it.

Authentication determines the Player. No caller supplied player ID is accepted in Player APIs, and a foreign record cannot appear in a successful current-Player response. Pending Outbox delivery remains `UNACQUIRED` until the grant transaction commits. FE should refetch the activation and owned-title views after a qualifying action, and may retry after a short delay. There is no new popup, notification, mail or reward in this activation. Expected ownership and state failures follow existing API error conventions; a pending event is not an error.

## Grant and reconciliation policy

LifeLog and Quest completion use their existing durable facts. Route completion and successful Mailbox Claim require dedicated typed facts written in the same transaction as their source mutation. These facts carry only stable source identity, Player, relevant classification and occurrence time; they do not copy LifeLog content, URL content or entities. Outbox retry and duplicate delivery converge through DB-backed event receipts and Player/definition uniqueness. Achievement and linked title are committed atomically, or recoverable through a durable child receipt. Admin grants are distinct from automatic acquisition evidence. Existing ownership is preserved without changing `acquiredAt`; admin revocation is respected and historical replay cannot silently regrant it.

A separate server-side reconciliation command supports dry-run, bounded apply and resume. It accepts existing durable facts or provider-confirmed historical completion/Claim state. Unknown evidence is skipped, never inferred from current Inventory or user-entered dates. Reconciliation records whether provenance was a durable event or confirmed historical state and does not invent an old event ID or backdate `acquiredAt`. It must not replay original reward or notification consumers. Removal, revocation and definition conflicts are excluded. Dedicated 19081 verification precedes any limited historical apply; 19080 and production are out of scope.

The ADMIN-only command is `POST /admin/v1/achievement-activation/reconcile` with `{ "afterPlayerId": 0, "batchSize": 100, "apply": false }`. A dry run is read only. The response includes `mode`, `nextAfterPlayerId`, `hasMore`, and per-Player `grantable`, `existing`, `revoked`, `unknown` counts. Apply uses the same request with `apply=true`, recomputes each Player under transaction, and can resume from the returned cursor. Batch size is 1–100. The only historical state fallback is QuestAcceptance `DONE` with `completed_at` and PlayerQuestRoute `COMPLETED` with `completed_at`. Earlier Mailbox claims without a retained durable fact remain unknown.

Before V50, a read-only 19081 catalog check found none of these seven stable codes, so no existing Korean name or definition row conflicts with the proposed V50 insert. This does not assert that the original full Achievement/Title catalog source files were obtained.
