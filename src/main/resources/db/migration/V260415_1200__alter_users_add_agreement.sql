ALTER TABLE users
    ADD COLUMN terms_of_service_agreed BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN privacy_policy_agreed   BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN location_terms_agreed   BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN terms_agreed_at         TIMESTAMP,
    ADD COLUMN marketing_agreed_at     TIMESTAMP;
