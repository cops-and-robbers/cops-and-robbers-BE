CREATE TABLE notices
(
    id         BIGSERIAL    PRIMARY KEY,
    title      VARCHAR(100) NOT NULL,
    content    TEXT         NOT NULL,
    pinned     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);
