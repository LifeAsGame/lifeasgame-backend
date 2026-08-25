# Admin Quest Acceptance override contract

## Endpoints

- `PATCH /admin/v1/quests/acceptances/{acceptanceId}/progress`
- `PATCH /admin/v1/quests/acceptances/{acceptanceId}/status`

Both are ADMIN-only Quest Acceptance runtime commands. They require a
single-line visible `reason` and an `Idempotency-Key`; `X-Correlation-Id` is
optional and a safe server UUID is generated when absent.

Progress is delta-only. Its body contains `delta >= 0` and `reason`; legacy
`type`, `value`, and body `idempotencyKey` fields are not part of the contract.
Status contains only `status` and `reason`, and continues to use the existing
`QuestStatus` parsing and aggregate transition rules, including legacy `DONE`
as the `COMPLETED` alias.

## Atomicity and concurrency

Both commands load the Acceptance with the existing pessimistic write lock.
The Acceptance mutation, existing Quest transition/completion outbox events,
and required Admin Audit row commit in one transaction. Audit or idempotency
persistence failure rolls all effects back.

The row lock serializes different overrides on one Acceptance, so progress
deltas cannot silently overwrite each other. The commands are deltas/domain
transitions, so no client `expectedVersion` contract is added; the existing
JPA `@Version` remains a secondary persistence guard.

## Durable idempotency and retries

The existing unique `admin_audit_events(action, idempotency_key)` constraint
is the durable success authority. Progress uses action
`QUEST_ACCEPTANCE_PROGRESS_ADJUST`; status uses
`QUEST_ACCEPTANCE_STATUS_CHANGE`, so the actions have separate key scopes.
A committed duplicate returns 409 without another Quest effect or response
snapshot replay.

An illegal domain transition or any failed transaction creates no success
Audit/idempotency row, so its key remains retryable. These commands never
select, advance, or otherwise mutate a QuestRoute.
