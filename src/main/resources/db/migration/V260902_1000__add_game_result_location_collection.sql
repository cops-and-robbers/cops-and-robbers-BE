ALTER TABLE game_results
    ADD COLUMN started_at                       TIMESTAMP,
    ADD COLUMN ended_at                         TIMESTAMP,
    ADD COLUMN location_reveal_interval_minutes INTEGER;
