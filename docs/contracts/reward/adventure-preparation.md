# Adventure preparation rewards

`Q_ADVENTURE_PREPARATION` (모험의 준비) is an optional ONCE Quest available to new and existing players. Its description is “일상의 활동을 세 번 기록하고, 다음 여정을 위한 보상을 받으세요.” Three distinct user-authored LifeLog creation facts after acceptance complete it. Edits, pre-acceptance records and redelivery do not advance it. The records are user reports, not external verification of real-life actions. Existing five Quests and Route/Step requirements are unchanged.

`RP_ADVENTURE_PREPARATION` grants `RD_ADVENTURE_GOLD` (GOLD 100) and `RD_RECORD_CRYSTAL` (`IT_RECORD_CRYSTAL`, one 기록 결정). This collectible is non-equipment, non-consumable, stackable to 99 and delivered unbound. Description: “활동 기록 퀘스트에서 얻는 수집품. 보관하거나 거래할 수 있습니다.” Existing reward items default to bound, including `IT_FIRST_STEP_FRAGMENT`. No use, crafting or enhancement effect is defined.

## Eligibility and transactions

The profile's `entitlement_code=ADVENTURE_PREPARATION` identifies an account-lifetime entitlement independently of display text, profile display versions, acceptance IDs, request keys and Player IDs. New display versions must retain this entitlement code. Character's internal lookup supplies the owning account; callers never supply reward amounts or recipients through the player API.

Settlement creation and the unique `(account_id, entitlement_code)` reservation commit together. A different settlement for the same account is `NOT_ELIGIBLE` with no reward lines. The reservation survives Player deletion/recreation and partial failure. Separate accounts remain separate; no real-world identity matching is performed.

Each line commits independently. GOLD processing locks the settlement, calls Economy's internal credit boundary, locks the Wallet and atomically commits the credit receipt, balance and line success. The receipt primary key is the reward line ID. Item processing continues to use the existing atomic Mailbox delivery receipt path. Concurrent duplicate processing returns the existing result. Missing receipts on successful lines fail closed rather than minting replacements.

Existing outbox delivery semantics remain unchanged: system failures propagate for outbox retry, recorded domain failures remain FAILED until the existing internal retry preparation resets that line to PENDING. Redelivery never resets a successful line. Once the failed line is prepared, replay processes the remaining work without duplicating either reward. There is no new public retry or grant endpoint.

## Read contract

`GET /api/v1/reward-settlements/quest-completions/{questAcceptanceId}` remains current-player scoped. `rewardType=GOLD, amount=100` describes the currency credit; the Item line exposes item ID/code and quantity. Each line retains its status and failure code.

- `PENDING`: at least one line is pending; rewards are not all settled.
- `PARTIAL_FAILED`: some lines succeeded and others failed.
- `FAILED`: all lines failed.
- `COMPLETED`: all lines succeeded (existing no-reward profiles remain completed).
- `NOT_ELIGIBLE`: this account already reserved this entitlement on another settlement; no new rewards.

A completed Quest does not imply a completed settlement. A settlement not yet created still uses the existing 404 response; clients must not interpret this as successful payout. ITEM success means Mailbox delivery, not Inventory claim or present ownership. GOLD success records a credit, not the current spendable balance. Item detail additionally exposes `description`.

## Marketplace boundary

Claiming the Mailbox reward makes the collectible available in Inventory. Trading is optional and never required for Quest completion or growth. Trading transfers only the item: personal LifeLogs, completed Quests and EXP remain unchanged. The existing seller fee is 100 basis points, rounded down in integer GOLD; price 100 therefore pays the seller 99 with fee 1. Listing prices remain user-selected.

This is an economy introduction, not a sustainable economy: collectible demand remains limited. GEM grants, payments/top-up, paid Shop fulfillment, equipment writes, crafting, enhancement and consumption are not enabled.
