# Admin Player holder grant contract

## Canonical commands

- `POST /admin/v1/players/{playerId}/achievements/{achievementId}`
- `POST /admin/v1/players/{playerId}/titles/{titleId}`

Both routes require ADMIN authority, a required safe `Idempotency-Key`, an
optional safe `X-Correlation-Id`, and the exact command body
`{"reason":"..."}`. Missing correlation is replaced with a server trace ID.
Reason is required, limited to 512 characters, must contain visible text, and
rejects control, format/bidi, and line/paragraph separator characters.

## Transaction and duplicate authority

`AdminPlayerHolderGrantService` composes exact Player/Definition validation,
the existing Character holder mutation, and the required success Audit in one
transaction. Holder rows are flushed before Audit append so relation conflicts
are decided by the existing database constraints:

- `uq_player_achv (player_id, achievement_id)`
- `uq_player_title (player_id, title_id)`

Durable command idempotency reuses
`uq_admin_audit_action_idempotency (action, idempotency_key)` with separate
actions `PLAYER_ACHIEVEMENT_GRANT` and `PLAYER_TITLE_GRANT`. An Audit or
idempotency failure rolls back the holder row. A holder duplicate records no
success Audit and is not reported as successful replay.

Audit targets identify the complete relation as `playerId:definitionId` with
target types `PLAYER_ACHIEVEMENT` and `PLAYER_TITLE`.

## Holder semantics

Title grant does not set or clear `Player.representativeTitleId`. Command
responses remain bounded receipts; the matching holder reads are the canonical
post-command state. Achievement/Title revoke and every Certification/Hobby
mutation remain gated.
