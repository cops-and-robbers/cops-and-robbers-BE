CREATE TABLE game_result_participants
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_result_id BIGINT       NOT NULL,
    user_id        BIGINT       NOT NULL,
    nickname       VARCHAR(20)  NOT NULL,
    team           VARCHAR(255) NOT NULL,
    status         VARCHAR(255) NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    CONSTRAINT fk_game_result_participants_game_result
        FOREIGN KEY (game_result_id) REFERENCES game_results (id)
);
