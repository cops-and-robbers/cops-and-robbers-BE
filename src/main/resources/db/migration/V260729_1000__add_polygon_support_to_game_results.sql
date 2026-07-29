ALTER TABLE game_results
    ADD COLUMN area_type                VARCHAR(10)            NOT NULL DEFAULT 'CIRCLE',
    ADD COLUMN playground_polygon       GEOMETRY(POLYGON, 4326),
    ALTER COLUMN playground_center      DROP NOT NULL,
    ALTER COLUMN playground_radius_in_meters DROP NOT NULL;

