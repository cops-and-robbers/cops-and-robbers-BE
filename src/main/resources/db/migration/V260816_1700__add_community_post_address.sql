ALTER TABLE community_posts ADD COLUMN address VARCHAR(255);
ALTER TABLE community_posts ADD COLUMN road_address VARCHAR(255);
ALTER TABLE community_posts ADD COLUMN building_name VARCHAR(255);
ALTER TABLE community_posts ADD COLUMN region VARCHAR(255);
ALTER TABLE community_posts ADD COLUMN place_name VARCHAR(50);

CREATE INDEX idx_community_posts_created_at_id ON community_posts (created_at DESC, id DESC);
