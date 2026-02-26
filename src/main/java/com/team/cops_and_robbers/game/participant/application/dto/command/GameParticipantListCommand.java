package com.team.cops_and_robbers.game.participant.application.dto.command;

public record GameParticipantListCommand(
        Long userId,
        Long gameId
) {
    public static GameParticipantListCommand of(Long userId, Long gameId) {
        return new GameParticipantListCommand(userId, gameId);
    }
}