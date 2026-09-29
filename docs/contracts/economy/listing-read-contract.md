# Marketplace listing read contract

## Consumer endpoints

All three endpoints require authentication and keep the existing success envelope.

| GET path | Result array | Existing scope |
| --- | --- | --- |
| `/api/v1/economy/listings` | `result.listings` | Listings stored as OPEN, including effective RESERVED listings |
| `/api/v1/economy/listings/me` | `result.listings` | Authenticated player's listings, including closed listings |
| `/api/v1/economy/listings/reservations` | `result.reservations` | Authenticated buyer's ACTIVE reservations |

These endpoints return the existing unpaged arrays. Player-scoped reads derive the player from authentication; a caller-supplied player ID does not select another owner's records. Existing ordering, reservation selection and transaction boundaries are unchanged. GET does not repair data, release reservations, persist changes or publish events.

`ListingSummary` and `ListingReservation` expose `saleQuantity` as an always-present nullable integer (`int32`, minimum 1 when known). It comes directly from `Listing.saleQuantity`, captured from the whole Inventory entry at listing creation. It is neither the seller's current Inventory quantity nor the Item's `maxStack`. `price` remains the total listing price in `currency`, not a unit price.

Historical rows without a quantity snapshot return explicit `"saleQuantity": null`, meaning **unknown**. They are not interpreted as quantity 1 and are not backfilled on reads. Existing write-side restrictions for incomplete historical snapshots remain unchanged.

The snapshot retains the same meaning in OPEN, effective RESERVED, SOLD and CANCELED states. The existing wire spelling is `CANCELED`. An ACTIVE reservation can make a stored OPEN listing appear RESERVED without changing the stored listing state. This does not add closed listings to the open-list endpoint. A completed sale keeps its original quantity even when the seller's entry is removed and the buyer's Inventory is merged or changed.

Reservation confirmation can display the selected listing's `saleQuantity` from a listing response. `GET /listings/reservations` also supplies the snapshot for existing reservation reads. `POST /listings/{listingId}/reserve` retains its existing token, hold ID and expiry acknowledgement; it does not become a listing detail response. The declared draft `ListingDetail` DTO does not establish an implemented detail endpoint.

## Item names

For a non-null `itemId`, consumers can use the existing authenticated `GET /api/v1/items/{itemId}` and read `result.name`. This is the current public Item catalog name, not a historical name snapshot. It does not expose the seller's private Inventory. Missing historical Item IDs must not be invented; catalog `maxStack` cannot fill an unknown sale quantity.

## Synthetic response examples

Known quantity (open listing or the corresponding element in the player's listings):

```json
{"isSuccess":true,"code":"COMMON-200","message":"성공입니다.","result":{"listings":[{"id":42,"itemId":7,"sellerId":21,"price":40,"currency":"GOLD","status":"OPEN","saleQuantity":7}]}}
```

Unknown historical quantity (the same explicit null applies to both response types):

```json
{"isSuccess":true,"code":"COMMON-200","message":"성공입니다.","result":{"reservations":[{"listingId":42,"itemId":7,"price":40,"currency":"GOLD","expiresAt":"2026-09-29T00:01:00Z","saleQuantity":null}]}}
```

These examples describe serialization, not permission to create a new reservation for an incomplete historical snapshot.
