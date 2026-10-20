ALTER TABLE game_results
    ADD COLUMN is_event_game BOOLEAN,
    ADD COLUMN host_user_id  BIGINT;
