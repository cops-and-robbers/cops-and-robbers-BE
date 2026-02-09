package com.team.cops_and_robbers.play.system.application.dto;

public record ArrestCommand(
        Long gameId,
        Long policeUserId,
        Long robberParticipantId
) {
    public static ArrestCommand of(Long gameId, Long policeUserId, Long robberParticipantId) {
        return new ArrestCommand(gameId, policeUserId, robberParticipantId);
    }
}
