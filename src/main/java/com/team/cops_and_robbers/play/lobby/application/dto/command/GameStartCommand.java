package com.team.cops_and_robbers.play.lobby.application.dto.command;

public record GameStartCommand(
        Long userId,
        Long gameId
) {
    public static GameStartCommand of(Long userId, Long gameId) {
        return new GameStartCommand(userId, gameId);
    }
}
