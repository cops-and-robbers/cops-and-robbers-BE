package com.team.cops_and_robbers.play.system.application.dto.command;

public record ArrestCommand(
        Long gameId,
        Long chaserUserId,
        Long runnerParticipantId
) {
    public static ArrestCommand of(Long gameId, Long chaserUserId, Long runnerParticipantId) {
        return new ArrestCommand(gameId, chaserUserId, runnerParticipantId);
    }
}
