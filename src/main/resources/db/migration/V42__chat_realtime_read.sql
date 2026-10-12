ALTER TABLE chat_messages
    ADD COLUMN client_message_id VARCHAR(80) NULL,
    ADD CONSTRAINT uq_chat_message_client_key UNIQUE (channel_id, sender_id, client_message_id);

ALTER TABLE channel_participants
    ADD COLUMN last_read_message_id BIGINT NULL;
