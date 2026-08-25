ALTER TABLE admin_audit_events
    ADD CONSTRAINT uq_admin_audit_action_idempotency
    UNIQUE (action, idempotency_key);
