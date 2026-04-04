CREATE TABLE reports
(
    id               BIGSERIAL    PRIMARY KEY,
    game_id          BIGINT       NOT NULL,
    reporter_user_id BIGINT       NOT NULL,
    reported_user_id BIGINT       NOT NULL,
    message_content  TEXT         NOT NULL,
    report_type      VARCHAR(255) NOT NULL,
    etc_reason       TEXT,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    CONSTRAINT uk_reporter_reported_game UNIQUE (reporter_user_id, reported_user_id, game_id)
);
