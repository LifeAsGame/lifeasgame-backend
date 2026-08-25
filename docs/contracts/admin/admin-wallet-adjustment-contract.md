# Admin Wallet adjustment contract

## Endpoint

`POST /admin/v1/economy/wallets/{playerId}/adjust` is an ADMIN-only delta
command. The request requires a single-line operator `reason` and an
`Idempotency-Key`; `X-Correlation-Id` is optional and a safe server UUID is
generated when it is absent.

## Atomic effect

The existing pessimistic Wallet lock protects the delta mutation. Wallet,
`WALLET_ADJUSTED` transactional outbox event, and the required Admin Audit row
commit in one transaction. Audit or idempotency persistence failure rolls all
of them back.

Admin Audit records `WALLET_ADJUSTMENT`, target type `WALLET`, the player ID
as target ID, the bounded reason, `SUCCESS`, correlation ID, and idempotency
key. Actor identity remains server-owned.

## Durable idempotency and retries

`admin_audit_events(action, idempotency_key)` is unique. Nullable keys remain
valid for other foundation callers, while this command requires a key. The
first committed key succeeds; a sequential or concurrent duplicate returns
409 and never commits a second Wallet effect. No response snapshot exists, so
the current balance is not replayed.

If Wallet business validation, Audit, or any other transaction step fails,
the Audit/idempotency row rolls back and the key may be retried. Wallet
adjustment is a pessimistically locked delta command, so no expected-version
contract is added.
