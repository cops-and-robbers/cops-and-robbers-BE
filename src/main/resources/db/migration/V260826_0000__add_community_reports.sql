CREATE TABLE community_post_reports
(
    id               BIGSERIAL    PRIMARY KEY,
    post_id          BIGINT       NOT NULL,
    post_title       VARCHAR(100) NOT NULL,
    post_content     TEXT         NOT NULL,
    reporter_user_id BIGINT       NOT NULL,
    reported_user_id BIGINT       NOT NULL,
    report_type      VARCHAR(255) NOT NULL,
    etc_reason       TEXT,
    status           VARCHAR(255) NOT NULL,
    admin_memo       TEXT,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    CONSTRAINT uk_post_reporter_reported UNIQUE (reporter_user_id, reported_user_id, post_id)
);

CREATE TABLE community_chat_reports
(
    id                BIGSERIAL    PRIMARY KEY,
    chat_message_id   BIGINT       NOT NULL,
    reporter_user_id  BIGINT       NOT NULL,
    reported_user_id  BIGINT       NOT NULL,
    message_content   TEXT         NOT NULL,
    report_type       VARCHAR(255) NOT NULL,
    etc_reason        TEXT,
    status            VARCHAR(255) NOT NULL,
    admin_memo        TEXT,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    CONSTRAINT uk_chat_reporter_reported UNIQUE (reporter_user_id, reported_user_id, chat_message_id)
);
