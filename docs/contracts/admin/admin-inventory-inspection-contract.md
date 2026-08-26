# Admin Inventory inspection contract

## Read surface

- `GET /admin/v1/items`
- `GET /admin/v1/items/{itemId}`
- `GET /admin/v1/players/{playerId}/inventory`
- `GET /admin/v1/players/{playerId}/mailbox`

All four routes are `SUPPORTED_READ + ALLOW`, remain protected by the central
`/admin/**` ADMIN authority rule, and add no mutation, Audit append,
idempotency metadata, lock, outbox, or migration.

Item search reuses `ItemQueryService.search` for name/category/type/rarity
parsing and filtering. Page is clamped to at least 0 and size to 1-100. Item
detail returns the requested definition identity, including canonical
`baseAttrs`.

Inventory and Mailbox reads pass the path `playerId` to
`InventoryQueryService.list(playerId)` and `MailboxQueryService.list(playerId)`.
Their top-level response repeats that server-owned `playerId` and exposes only
operational entry fields. Per-instance `instanceAttrs` is never mapped to the
Admin response.

## FE handoff

These reads support the future operator flow:

```text
Player verify
-> Item search/detail verify
-> Inventory/Mailbox inspect
-> entitlement review
-> entitlement command
-> Inventory/Mailbox reload
-> Audit reconciliation
```

This contract does not define frontend routes or deep links.
