CREATE TABLE community_notifications
(
    id                BIGSERIAL    PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    type              VARCHAR(255) NOT NULL,
    community_post_id BIGINT       NOT NULL,
    post_title        VARCHAR(100) NOT NULL,
    content           VARCHAR(500) NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL
);

CREATE INDEX idx_community_notifications_user_id_id
    ON community_notifications (user_id, id DESC);

CREATE TABLE community_post_notification_settings
(
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT    NOT NULL,
    community_post_id BIGINT    NOT NULL,
    notify_comments   BOOLEAN   NOT NULL,
    notify_replies    BOOLEAN   NOT NULL,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL,
    CONSTRAINT uk_post_noti_setting_user_post UNIQUE (user_id, community_post_id)
);

ALTER TABLE users
    ADD COLUMN allow_community_push           BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN community_notification_read_at TIMESTAMP;
