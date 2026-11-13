ALTER TABLE game_areas DROP CONSTRAINT chk_circle_fields;
ALTER TABLE game_areas DROP CONSTRAINT chk_polygon_fields;

ALTER TABLE game_areas ADD CONSTRAINT chk_circle_fields
    CHECK (
        area_type != 'CIRCLE' OR (
            playground_center IS NOT NULL AND
            playground_radius_in_meters IS NOT NULL
        )
    );

ALTER TABLE game_areas ADD CONSTRAINT chk_polygon_fields
    CHECK (
        area_type != 'POLYGON' OR playground_polygon IS NOT NULL
    );
