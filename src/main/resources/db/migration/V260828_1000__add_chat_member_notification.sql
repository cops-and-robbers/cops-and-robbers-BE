ALTER TABLE community_chat_members
    ADD COLUMN allow_notification   BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN last_read_message_id BIGINT;
