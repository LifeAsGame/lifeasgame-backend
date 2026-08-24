ALTER TABLE users
    ADD COLUMN account_authority VARCHAR(16) NOT NULL DEFAULT 'USER'
        AFTER status,
    ADD CONSTRAINT ck_user_account_authority CHECK (
        account_authority IN ('USER', 'ADMIN')
    );
