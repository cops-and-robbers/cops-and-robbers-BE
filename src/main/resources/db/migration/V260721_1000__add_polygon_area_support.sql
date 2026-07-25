ALTER TABLE game_areas
    ADD COLUMN area_type                VARCHAR(10)            NOT NULL DEFAULT 'CIRCLE',
    ADD COLUMN playground_polygon       GEOMETRY(POLYGON, 4326),
    ADD COLUMN jail_polygon             GEOMETRY(POLYGON, 4326),
    ALTER COLUMN playground_center      DROP NOT NULL,
    ALTER COLUMN playground_radius_in_meters DROP NOT NULL,
    ALTER COLUMN jail_center            DROP NOT NULL,
    ALTER COLUMN jail_radius_in_meters  DROP NOT NULL;

ALTER TABLE game_areas ADD CONSTRAINT chk_circle_fields
    CHECK (
        area_type != 'CIRCLE' OR (
            playground_center IS NOT NULL AND
            playground_radius_in_meters IS NOT NULL AND
            jail_center IS NOT NULL AND
            jail_radius_in_meters IS NOT NULL
        )
    );

ALTER TABLE game_areas ADD CONSTRAINT chk_polygon_fields
    CHECK (
        area_type != 'POLYGON' OR (
            playground_polygon IS NOT NULL AND
            jail_polygon IS NOT NULL
        )
    );
