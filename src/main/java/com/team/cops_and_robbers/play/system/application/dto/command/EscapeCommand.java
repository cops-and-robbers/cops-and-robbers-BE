package com.team.cops_and_robbers.play.system.application.dto.command;

public record EscapeCommand(
        Long gameId,
        Long escapedRunnerUserId
) {
    public static EscapeCommand of(Long gameId, Long escapedRunnerUserId) {
        return new EscapeCommand(gameId, escapedRunnerUserId);
    }
}
