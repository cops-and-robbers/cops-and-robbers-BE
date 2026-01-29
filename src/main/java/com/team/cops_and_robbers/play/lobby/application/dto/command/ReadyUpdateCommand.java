package com.team.cops_and_robbers.play.lobby.application.dto.command;

public record ReadyUpdateCommand(
        Long userId,
        Long gameId,
        boolean isReady
) {
    public static ReadyUpdateCommand of(Long userId, Long gameId, boolean isReady) {
        return new ReadyUpdateCommand(userId, gameId, isReady);
    }
}
