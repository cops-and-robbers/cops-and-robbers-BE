package com.team.cops_and_robbers.game.participant.application.dto.result;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;

public record GameJoinResult(
        Long gameId,
        Long participantId,
        boolean isEventGame
) {
    public static GameJoinResult from(GameParticipant participant) {
        return new GameJoinResult(
                participant.getGame().getId(),
                participant.getId(),
                participant.getGame().isEventGame()
        );
    }
}