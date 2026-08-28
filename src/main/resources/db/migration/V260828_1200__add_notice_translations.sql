CREATE TABLE notice_translations
(
    id         BIGSERIAL    PRIMARY KEY,
    notice_id  BIGINT       NOT NULL,
    language   VARCHAR(5)   NOT NULL,
    title      VARCHAR(100) NOT NULL,
    content    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    CONSTRAINT uk_notice_translation_notice_language UNIQUE (notice_id, language),
    CONSTRAINT fk_notice_translation_notice
        FOREIGN KEY (notice_id) REFERENCES notices (id) ON DELETE CASCADE
);

INSERT INTO notice_translations (notice_id, language, title, content, created_at, updated_at)
SELECT id, 'KO', title, content, created_at, updated_at
FROM notices;

ALTER TABLE notices
    ADD COLUMN original_language VARCHAR(5) NOT NULL DEFAULT 'KO';

ALTER TABLE notices
    DROP COLUMN title;

ALTER TABLE notices
    DROP COLUMN content;
