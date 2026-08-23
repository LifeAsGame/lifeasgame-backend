ALTER TABLE trades
    ADD COLUMN item_id BIGINT NULL AFTER item_inst_id,
    ADD COLUMN sale_quantity INTEGER NULL AFTER item_id,
    ADD CONSTRAINT ck_trade_sale_quantity CHECK (
        sale_quantity IS NULL OR sale_quantity > 0
    );
