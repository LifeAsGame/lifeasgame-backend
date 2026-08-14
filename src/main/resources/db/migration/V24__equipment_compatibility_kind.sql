ALTER TABLE items
    ADD COLUMN equipment_compatibility_kind ENUM (
        'WEAPON',
        'HEAD',
        'CHEST',
        'LEGS',
        'HANDS',
        'FEET',
        'NECK',
        'RING',
        'TRINKET'
    ) NULL AFTER type;

UPDATE items
SET equipment_compatibility_kind = CASE
    WHEN category = 'WEAPON' THEN 'WEAPON'
    WHEN category = 'ARMOR' AND type = 'HELMET' THEN 'HEAD'
    WHEN category = 'ARMOR' AND type = 'CHEST' THEN 'CHEST'
    WHEN category = 'ACCESSORY' AND type = 'RING' THEN 'RING'
END
WHERE category = 'WEAPON'
   OR (category = 'ARMOR' AND type IN ('HELMET', 'CHEST'))
   OR (category = 'ACCESSORY' AND type = 'RING');
