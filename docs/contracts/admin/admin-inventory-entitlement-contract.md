# Admin Inventory entitlement contract

## Endpoints

- `POST /admin/v1/players/{playerId}/inventory/add`
- `POST /admin/v1/players/{playerId}/mailbox/deliver`

Both ADMIN-only commands require `Idempotency-Key` and a single-line visible
`reason` without Unicode control, format, line-separator, or paragraph-separator
characters. `X-Correlation-Id` is optional; a safe server UUID is generated
when absent.

Both bodies contain exactly `itemId`, positive `quantity`, `bound`, and
`reason`. `instanceAttrs` is not an Admin input. The current `InstanceAttrs`
domain type accepts an unbounded `Map<String, Object>` and only copies,
serializes, and compares it as a stack key; neither Item nor ItemCarryPolicy
defines an attribute schema. Admin entitlements therefore use canonical empty
instance attributes instead of accepting arbitrary operator JSON.

`bound` remains an explicit input because Inventory and Mailbox persist it as
entitlement state, include it in stack identity, and preserve it during mailbox
claim. Inventory item-added events also carry the bound fact.

## Atomicity and durable idempotency

`AdminInventoryEntitlementService` invokes the provider-owned Inventory or
Mailbox mutation and appends the required success Audit in one transaction.
Inventory preserves its existing item-added outbox event; Mailbox delivery has
no existing entitlement event to add.

Inventory uses Audit action `INVENTORY_ITEM_ADD` and target
`PLAYER_INVENTORY`. Mailbox uses `MAILBOX_ITEM_DELIVERY` and target
`PLAYER_MAILBOX`. The existing unique `(action, idempotency_key)` constraint is
the durable success authority, so the same textual key is valid once in each
separate action scope. A committed duplicate returns conflict without a second
entitlement; no response snapshot replay is provided.

The existing pessimistic write locks on `PlayerInventory` and `PlayerMailbox`
serialize entitlement mutations and protect stack/slot capacity. Business or
Audit failure rolls back the entitlement, Inventory outbox work, and success
Audit, leaving the key retryable.
