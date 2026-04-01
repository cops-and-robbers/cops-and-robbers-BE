CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE users
(
    id                   BIGSERIAL    PRIMARY KEY,
    social_id            VARCHAR(100) NOT NULL,
    social_type          VARCHAR(10)  NOT NULL,
    nickname             VARCHAR(30)  NOT NULL UNIQUE,
    allow_game_push      BOOLEAN      NOT NULL,
    allow_marketing_push BOOLEAN      NOT NULL,
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP    NOT NULL,
    CONSTRAINT uk_users_social_id_type UNIQUE (social_id, social_type)
);

CREATE TABLE games
(
    id                               BIGSERIAL    PRIMARY KEY,
    invite_code                      VARCHAR(255) NOT NULL UNIQUE,
    status                           VARCHAR(255) NOT NULL,
    round_duration_minutes           INTEGER      NOT NULL,
    location_reveal_interval_minutes INTEGER      NOT NULL,
    police_wait_minutes              INTEGER      NOT NULL,
    max_participants                 INTEGER      NOT NULL,
    started_at                       TIMESTAMP,
    last_ended_at                    TIMESTAMP,
    created_at                       TIMESTAMP    NOT NULL,
    updated_at                       TIMESTAMP    NOT NULL
);

CREATE TABLE user_devices
(
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES users (id),
    device_id   VARCHAR(255) NOT NULL,
    device_type VARCHAR(10)  NOT NULL,
    fcm_token   VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE TABLE game_areas
(
    id                          BIGSERIAL             PRIMARY KEY,
    game_id                     BIGINT                NOT NULL UNIQUE REFERENCES games (id),
    playground_center           GEOMETRY(POINT, 4326) NOT NULL,
    playground_radius_in_meters INTEGER               NOT NULL,
    jail_center                 GEOMETRY(POINT, 4326) NOT NULL,
    jail_radius_in_meters       INTEGER               NOT NULL,
    created_at                  TIMESTAMP             NOT NULL,
    updated_at                  TIMESTAMP             NOT NULL
);

CREATE TABLE participants
(
    id         BIGSERIAL    PRIMARY KEY,
    game_id    BIGINT       NOT NULL REFERENCES games (id),
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    team       VARCHAR(255),
    status     VARCHAR(255) NOT NULL,
    is_ready   BOOLEAN      NOT NULL,
    is_host    BOOLEAN      NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE TABLE game_results
(
    id                          BIGSERIAL             PRIMARY KEY,
    game_id                     BIGINT                NOT NULL,
    winner_team                 VARCHAR(255)          NOT NULL,
    end_reason                  VARCHAR(255)          NOT NULL,
    total_police_count          INTEGER               NOT NULL,
    total_robber_count          INTEGER               NOT NULL,
    arrested_robber_count       INTEGER               NOT NULL,
    duration_seconds            INTEGER               NOT NULL,
    highlights                  JSONB,
    playground_center           GEOMETRY(POINT, 4326) NOT NULL,
    playground_radius_in_meters INTEGER               NOT NULL,
    created_at                  TIMESTAMP             NOT NULL,
    updated_at                  TIMESTAMP             NOT NULL
);
