ALTER TABLE game_result_participants
    ADD COLUMN radio_seconds        INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN radio_last_issued_at TIMESTAMP;
