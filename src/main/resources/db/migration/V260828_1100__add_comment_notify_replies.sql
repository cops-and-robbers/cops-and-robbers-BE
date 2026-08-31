ALTER TABLE community_comments
    ADD COLUMN notify_replies BOOLEAN NOT NULL DEFAULT TRUE;
