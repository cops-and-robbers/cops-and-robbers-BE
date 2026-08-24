CREATE TABLE community_comments
(
    id                BIGSERIAL PRIMARY KEY,
    community_post_id BIGINT       NOT NULL,
    parent_id         BIGINT,
    user_id           BIGINT       NOT NULL,
    content           VARCHAR(500) NOT NULL,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL
);

CREATE INDEX idx_comments_post_parent_id ON community_comments (community_post_id, parent_id, id);
CREATE INDEX idx_comments_parent_id ON community_comments (parent_id);

CREATE TABLE community_post_likes
(
    id                BIGSERIAL PRIMARY KEY,
    community_post_id BIGINT    NOT NULL,
    user_id           BIGINT    NOT NULL,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL,
    CONSTRAINT uk_post_like_post_user UNIQUE (community_post_id, user_id)
);

CREATE INDEX idx_post_likes_post_id ON community_post_likes (community_post_id);

CREATE TABLE community_post_scraps
(
    id                BIGSERIAL PRIMARY KEY,
    community_post_id BIGINT    NOT NULL,
    user_id           BIGINT    NOT NULL,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL,
    CONSTRAINT uk_post_scrap_post_user UNIQUE (community_post_id, user_id)
);

CREATE INDEX idx_post_scraps_post_id ON community_post_scraps (community_post_id);

CREATE INDEX idx_post_scraps_user_id_id ON community_post_scraps (user_id, id DESC);
