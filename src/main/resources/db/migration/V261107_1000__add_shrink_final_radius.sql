ALTER TABLE games
    ADD COLUMN shrink_final_radius_in_meters INTEGER;

ALTER TABLE game_results
    ADD COLUMN shrink_final_radius_in_meters INTEGER;
