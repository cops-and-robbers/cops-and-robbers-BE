CREATE TABLE bug_reports (
    id         BIGSERIAL PRIMARY KEY,
    content    TEXT      NOT NULL,
    user_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
