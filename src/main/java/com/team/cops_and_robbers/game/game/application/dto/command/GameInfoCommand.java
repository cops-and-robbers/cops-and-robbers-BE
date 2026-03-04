package com.team.cops_and_robbers.game.game.application.dto.command;

public record GameInfoCommand(
        Long userId,
        Long gameId
) {
    public static GameInfoCommand of(Long userId, Long gameId) {
        return new GameInfoCommand(userId, gameId);
    }
}
