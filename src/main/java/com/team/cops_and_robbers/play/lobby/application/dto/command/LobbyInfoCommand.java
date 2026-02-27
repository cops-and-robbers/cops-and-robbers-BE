package com.team.cops_and_robbers.play.lobby.application.dto.command;

public record LobbyInfoCommand(
        Long userId,
        Long gameId
) {
    public static LobbyInfoCommand of(Long userId, Long gameId) {
        return new LobbyInfoCommand(userId, gameId);
    }
}
