ALTER TABLE game_results
    DROP COLUMN IF EXISTS highlights;

ALTER TABLE games
    ADD COLUMN total_arrest_count INT NOT NULL DEFAULT 0;
ALTER TABLE game_results
    ADD COLUMN total_arrest_count INT NOT NULL DEFAULT 0;
