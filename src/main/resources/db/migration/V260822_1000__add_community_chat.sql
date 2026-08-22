CREATE TABLE community_chat_members
(
    id                BIGSERIAL PRIMARY KEY,
    community_post_id BIGINT    NOT NULL,
    user_id           BIGINT    NOT NULL,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL,
    CONSTRAINT uk_chat_member_post_user UNIQUE (community_post_id, user_id)
);

CREATE INDEX idx_chat_members_post_id ON community_chat_members (community_post_id);
CREATE INDEX idx_chat_members_user_id ON community_chat_members (user_id);

CREATE TABLE community_chat_messages
(
    id                BIGSERIAL    PRIMARY KEY,
    message_key       VARCHAR(36)  NOT NULL,
    community_post_id BIGINT       NOT NULL,
    sender_id         BIGINT       NOT NULL,
    sender_nickname   VARCHAR(30)  NOT NULL,
    message           VARCHAR(500) NOT NULL,
    message_type      VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL
);

CREATE INDEX idx_chat_messages_post_id_id ON community_chat_messages (community_post_id, id DESC);