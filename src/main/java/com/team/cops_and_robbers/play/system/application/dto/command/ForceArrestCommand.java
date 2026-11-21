package com.team.cops_and_robbers.play.system.application.dto.command;

public record ForceArrestCommand(
        Long gameId,
        Long runnerUserId
) {
    public static ForceArrestCommand of(Long gameId, Long runnerUserId) {
        return new ForceArrestCommand(gameId, runnerUserId);
    }
}
