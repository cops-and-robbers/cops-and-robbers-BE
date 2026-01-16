package com.team.cops_and_robbers.game.participant.presentation.dto.response;

import com.team.cops_and_robbers.game.participant.application.dto.result.GameJoinResult;

public record GameJoinResponse(
        Long gameId,
        Long participantId
) {
    public static GameJoinResponse from(GameJoinResult result) {
        return new GameJoinResponse(
                result.gameId(),
                result.participantId()
        );
    }
}
