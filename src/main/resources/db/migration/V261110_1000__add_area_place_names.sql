CREATE TABLE area_place_names
(
    id             BIGSERIAL    PRIMARY KEY,
    coordinate_key VARCHAR(20)  NOT NULL UNIQUE,
    place_name     VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL
);
