ALTER TABLE player_equipment
    ADD CONSTRAINT uq_player_equipment_item
    UNIQUE (player_id, item_inst_id);
