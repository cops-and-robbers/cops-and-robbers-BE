package com.team.cops_and_robbers.game.area.application.dto.command;

public record GameAreaCommand(
        Long userId,
        Long gameId
) {
    public static GameAreaCommand of(Long userId, Long gameId) {
        return new GameAreaCommand(userId, gameId);
    }
}