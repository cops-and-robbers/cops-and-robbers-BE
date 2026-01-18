package com.team.cops_and_robbers.game.participant.application.dto.command;

public record GameLeaveCommand(
        Long userId,
        Long gameId
) {
    public static GameLeaveCommand of(Long userId, Long gameId) {
        return new GameLeaveCommand(userId, gameId);
    }
}