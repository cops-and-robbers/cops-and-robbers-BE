CREATE TABLE community_posts
(
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    title            VARCHAR(100) NOT NULL,
    content          TEXT         NOT NULL,
    meeting_at       TIMESTAMP    NOT NULL,
    latitude         DOUBLE PRECISION NOT NULL,
    longitude        DOUBLE PRECISION NOT NULL,
    max_participants INTEGER      NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'RECRUITING',
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL
);
