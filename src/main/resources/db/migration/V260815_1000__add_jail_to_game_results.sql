ALTER TABLE game_results
    ADD COLUMN jail_center           GEOMETRY(POINT, 4326),
    ADD COLUMN jail_radius_in_meters INTEGER,
    ADD COLUMN jail_polygon          GEOMETRY(POLYGON, 4326);
