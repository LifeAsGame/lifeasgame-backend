# Admin Audit foundation contract

## Purpose

`adminaudit` is the durable accountability capability for approved high-risk
Admin commands. It is not gameplay history, an application/access log, or a
business-domain event store.

## Safe append fields

The provider-owned `AdminAuditInternalApi` accepts only action, target
type/id, reason, result, correlation ID, and an optional idempotency key.
The authenticated account ID and occurrence time are server-owned.

Audit records never contain raw JWT/token values, passwords, Journal or
LifeLog bodies, Person private fields, Direct Chat messages, full
request/response payloads, arbitrary entity serialization, or generic JSON
metadata.

## Transaction contract

`append` requires an existing caller transaction. An approved high-risk
command must perform its mutation and required audit append in that same
transaction and must not swallow append failures. A missing transaction or
failed audit persistence aborts the command transaction. Rolled-back attempts
do not retain a misleading same-transaction audit row; separate failed-attempt
auditing is outside this foundation.

Reason is nullable at foundation level only. Unit E must make it mandatory
where each high-risk command contract requires it and must supply its existing
correlation/idempotency values without inventing global duplicate prevention.

## Reader contract

`GET /admin/v1/audit-events` is ADMIN-only and returns safe fields ordered by
`occurredAt DESC, id DESC`. It supports actor, action, target type/id, result,
correlation ID, an inclusive `from`, exclusive `to`, opaque cursor, and a
server-bounded page size of 1-100. The response contains `items` and
`nextCursor`; no unbounded listing or delete/update endpoint exists.
