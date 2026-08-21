ALTER TABLE listings
    ADD COLUMN sale_quantity INTEGER NULL AFTER item_id,
    ADD CONSTRAINT ck_listing_sale_quantity CHECK (
        sale_quantity IS NULL OR sale_quantity > 0
    );

CREATE TABLE listing_reservations (
    active_flag INTEGER,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    buyer_player_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT,
    reservation_token VARCHAR(36) NOT NULL,
    wallet_hold_id VARCHAR(36) NOT NULL,
    state VARCHAR(16) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_listing_reservation_active
        UNIQUE (listing_id, active_flag),
    CONSTRAINT uq_listing_reservation_token
        UNIQUE (reservation_token),
    CONSTRAINT ck_listing_reservation_state CHECK (
        state IN ('ACTIVE', 'EXPIRED', 'CONSUMED')
    ),
    CONSTRAINT ck_listing_reservation_active_flag CHECK (
        (state = 'ACTIVE' AND active_flag = 1)
        OR (state IN ('EXPIRED', 'CONSUMED') AND active_flag IS NULL)
    ),
    CONSTRAINT fk_listing_reservation_listing
        FOREIGN KEY (listing_id) REFERENCES listings (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_listing_reservation_expiry
    ON listing_reservations (state, expires_at);

CREATE INDEX idx_listing_reservation_buyer
    ON listing_reservations (buyer_player_id, state);

INSERT INTO listing_reservations (
    active_flag,
    created_at,
    expires_at,
    buyer_player_id,
    listing_id,
    updated_at,
    version,
    reservation_token,
    wallet_hold_id,
    state
)
SELECT
    1,
    listing.created_at,
    listing.reservation_expires_at,
    listing.reserved_by,
    listing.id,
    listing.updated_at,
    0,
    listing.reservation_token,
    listing.reserved_hold_id,
    'ACTIVE'
FROM listings listing
JOIN wallet_holds hold
  ON hold.hold_id = listing.reserved_hold_id
 AND hold.status = 'OPEN'
 AND hold.amount = listing.price
 AND hold.currency = listing.currency
 AND hold.expires_at >= listing.reservation_expires_at
JOIN wallets wallet
  ON wallet.id = hold.wallet_id
 AND wallet.owner_id = listing.reserved_by
WHERE listing.status = 'RESERVED'
  AND listing.active_flag = 1
  AND listing.reserved_by IS NOT NULL
  AND listing.reservation_token IS NOT NULL
  AND listing.reservation_expires_at IS NOT NULL
  AND listing.reserved_hold_id IS NOT NULL;

UPDATE listings listing
JOIN listing_reservations reservation
  ON reservation.listing_id = listing.id
 AND reservation.state = 'ACTIVE'
SET listing.status = 'OPEN',
    listing.reserved_by = NULL,
    listing.reservation_token = NULL,
    listing.reservation_expires_at = NULL,
    listing.reserved_hold_id = NULL;

CREATE TEMPORARY TABLE v27_invalid_legacy_holds AS
SELECT DISTINCT
    hold.id AS hold_row_id,
    hold.wallet_id,
    hold.currency,
    hold.amount
FROM listings listing
JOIN wallet_holds hold
  ON hold.hold_id = listing.reserved_hold_id
 AND hold.status = 'OPEN'
JOIN wallets wallet
  ON wallet.id = hold.wallet_id
 AND wallet.owner_id = listing.reserved_by
LEFT JOIN listing_reservations reservation
  ON reservation.listing_id = listing.id
 AND reservation.state = 'ACTIVE'
WHERE listing.status = 'RESERVED'
  AND listing.active_flag = 1
  AND reservation.id IS NULL;

INSERT INTO wallet_balances (
    amount,
    created_at,
    updated_at,
    wallet_id,
    currency
)
SELECT
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    invalid.wallet_id,
    invalid.currency
FROM v27_invalid_legacy_holds invalid
LEFT JOIN wallet_balances balance
  ON balance.wallet_id = invalid.wallet_id
 AND balance.currency = invalid.currency
WHERE balance.id IS NULL
GROUP BY invalid.wallet_id, invalid.currency;

CREATE TEMPORARY TABLE v27_invalid_wallet_refunds AS
SELECT
    wallet_id,
    currency,
    SUM(amount) AS amount
FROM v27_invalid_legacy_holds
GROUP BY wallet_id, currency;

UPDATE wallet_balances balance
JOIN v27_invalid_wallet_refunds refund
  ON refund.wallet_id = balance.wallet_id
 AND refund.currency = balance.currency
JOIN v27_invalid_legacy_holds invalid
  ON invalid.wallet_id = refund.wallet_id
 AND invalid.currency = refund.currency
JOIN wallet_holds hold
  ON hold.id = invalid.hold_row_id
 AND hold.status = 'OPEN'
SET balance.amount = balance.amount + refund.amount,
    balance.updated_at = CURRENT_TIMESTAMP(6),
    hold.status = 'CANCELED',
    hold.updated_at = CURRENT_TIMESTAMP(6);

DROP TEMPORARY TABLE v27_invalid_wallet_refunds;
DROP TEMPORARY TABLE v27_invalid_legacy_holds;

UPDATE listings listing
LEFT JOIN listing_reservations reservation
  ON reservation.listing_id = listing.id
 AND reservation.state = 'ACTIVE'
SET listing.status = 'OPEN',
    listing.reserved_by = NULL,
    listing.reservation_token = NULL,
    listing.reservation_expires_at = NULL,
    listing.reserved_hold_id = NULL
WHERE listing.status = 'RESERVED'
  AND listing.active_flag = 1
  AND reservation.id IS NULL;

UPDATE inventory_entries entry
JOIN listings listing
  ON listing.item_inst_id = entry.id
 AND listing.seller_player_id = entry.player_id
SET entry.availability = 'LISTED'
WHERE listing.status = 'OPEN'
  AND listing.active_flag = 1
  AND entry.availability = 'FREE';

UPDATE inventory_entries entry
JOIN listings listing
  ON listing.item_inst_id = entry.id
 AND listing.seller_player_id = entry.player_id
LEFT JOIN listing_reservations reservation
  ON reservation.listing_id = listing.id
 AND reservation.state = 'ACTIVE'
SET entry.availability = 'LISTED'
WHERE listing.status = 'OPEN'
  AND listing.active_flag = 1
  AND reservation.id IS NULL
  AND entry.availability = 'RESERVED_FOR_TRADE';

UPDATE inventory_entries entry
JOIN listings listing
  ON listing.item_inst_id = entry.id
 AND listing.seller_player_id = entry.player_id
JOIN listing_reservations reservation
  ON reservation.listing_id = listing.id
 AND reservation.state = 'ACTIVE'
SET entry.availability = 'RESERVED_FOR_TRADE'
WHERE entry.availability = 'LISTED';
