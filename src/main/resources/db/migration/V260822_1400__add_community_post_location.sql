ALTER TABLE community_posts
    ADD COLUMN location GEOMETRY(POINT, 4326);

UPDATE community_posts
SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326);

ALTER TABLE community_posts
    ALTER COLUMN location SET NOT NULL;

CREATE INDEX idx_community_posts_location ON community_posts USING GIST (location);

ALTER TABLE community_posts
    DROP COLUMN latitude,
    DROP COLUMN longitude;
