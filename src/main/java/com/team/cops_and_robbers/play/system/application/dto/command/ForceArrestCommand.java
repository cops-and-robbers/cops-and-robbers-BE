package com.team.cops_and_robbers.play.system.application.dto.command;

public record ForceArrestCommand(
        Long gameId,
        Long robberUserId
) {
    public static ForceArrestCommand of(Long gameId, Long robberUserId) {
        return new ForceArrestCommand(gameId, robberUserId);
    }
}
