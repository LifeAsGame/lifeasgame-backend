# Admin Player holder inspection contract

## Canonical reads

- `GET /admin/v1/players/{playerId}/achievements`
- `GET /admin/v1/players/{playerId}/certifications`
- `GET /admin/v1/players/{playerId}/hobbies`
- `GET /admin/v1/players/{playerId}/titles`

Each route requires ADMIN authority, uses the positive path `playerId` as the
exact query target, and echoes that server-owned value in `result.playerId`.
`PlayerHolderQueryService` delegates to the existing Character Readers with
that explicit id; it does not use `CurrentPlayerAccessor` or player-facing HTTP.

## Bounded holder fields

- Achievement: `achievementId`, `code`, `name`, `category`, `acquiredAt`
- Certification: `certificationId`, `name`, `issuer`, `category`,
  `acquiredDate`, `expiresDate`, `grantedAt`
- Hobby: `hobbyId`, definition `name`, `category`, `proficiency`, `status`,
  `startedOn`, `xp`
- Title: `titleId`, `code`, `name`, `category`, `acquiredAt`

Achievement and Title `descMd` remain definition-detail content and are not
copied into holder summaries. Hobby `detail` is excluded. Hobby `customName`
is also excluded because the existing player create/update requests accept it
as player-authored free-form content. Title representative state is not
inferred from ownership.

## Mutation gate

These reads satisfy only the holder-query prerequisite. Existing grant/revoke
routes remain `GATED_HIGH_RISK + GATE`. Future AR-012 commands still require an
Admin-specific command boundary, reason, durable idempotency, Admin Audit,
duplicate and concurrent-holder semantics, rollback, input validation,
representative-title side-effect review, and focused persistence proof where
needed. This read contract adds no mutation, lock, audit write, or migration.
