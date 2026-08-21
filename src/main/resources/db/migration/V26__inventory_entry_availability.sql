ALTER TABLE inventory_entries
    ADD COLUMN availability VARCHAR(32) NULL AFTER rarity;

UPDATE inventory_entries entry
SET entry.availability = CASE
    WHEN EXISTS (
        SELECT 1
        FROM player_equipment equipment
        WHERE equipment.player_id = entry.player_id
          AND equipment.item_inst_id = entry.id
    ) THEN 'EQUIPPED'
    ELSE 'FREE'
END;

ALTER TABLE inventory_entries
    MODIFY COLUMN availability VARCHAR(32) NOT NULL DEFAULT 'FREE',
    ADD CONSTRAINT ck_inventory_entry_availability CHECK (
        availability IN (
            'FREE',
            'EQUIPPED',
            'LOCKED',
            'LISTED',
            'RESERVED_FOR_TRADE',
            'TRANSFER_PROCESSING'
        )
    );
