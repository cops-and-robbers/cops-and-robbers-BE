package com.team.cops_and_robbers.history.application.dto.command;

public record GameResultCommand(
        Long userId,
        Long gameResultId
) {
    public static GameResultCommand of(Long userId, Long gameResultId) {
        return new GameResultCommand(userId, gameResultId);
    }
}
