ALTER TABLE game_result_participants
    ADD COLUMN is_mvp          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN distance_meters DOUBLE PRECISION;

CREATE INDEX idx_game_result_participants_game_result_id
    ON game_result_participants (game_result_id);
