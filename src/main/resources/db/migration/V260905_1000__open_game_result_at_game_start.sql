ALTER TABLE game_results
    ALTER COLUMN winner_team           DROP NOT NULL,
    ALTER COLUMN end_reason            DROP NOT NULL,
    ALTER COLUMN total_police_count    DROP NOT NULL,
    ALTER COLUMN total_robber_count    DROP NOT NULL,
    ALTER COLUMN arrested_robber_count DROP NOT NULL,
    ALTER COLUMN total_arrest_count    DROP NOT NULL,
    ALTER COLUMN duration_seconds      DROP NOT NULL;

CREATE UNIQUE INDEX uq_game_results_in_progress
    ON game_results (game_id)
    WHERE end_reason IS NULL;

ALTER TABLE game_result_participants
    ADD COLUMN left_at TIMESTAMP;
