CREATE TABLE community_chat_pins
(
    id                BIGSERIAL    PRIMARY KEY,
    community_post_id BIGINT       NOT NULL UNIQUE,
    writer_id         BIGINT       NOT NULL,
    content           VARCHAR(500) NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL
);
