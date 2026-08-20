ALTER TABLE community_posts ADD COLUMN address VARCHAR(255);
ALTER TABLE community_posts ADD COLUMN road_address VARCHAR(255);
ALTER TABLE community_posts ADD COLUMN building_name VARCHAR(255);
ALTER TABLE community_posts ADD COLUMN region VARCHAR(255);
ALTER TABLE community_posts ADD COLUMN place_name VARCHAR(50);
UPDATE community_posts SET place_name = '미정' WHERE place_name IS NULL;
ALTER TABLE community_posts ALTER COLUMN place_name SET NOT NULL;
ALTER TABLE community_posts ADD COLUMN country_code VARCHAR(2);

CREATE INDEX idx_community_posts_country_created_at_id
    ON community_posts (country_code, created_at DESC, id DESC);
