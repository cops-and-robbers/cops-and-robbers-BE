package com.team.cops_and_robbers.game.participant.presentation.dto.response;

import com.team.cops_and_robbers.game.participant.application.dto.result.GameLeaveResult;

public record GameLeaveResponse(
        Long leftUserId,
        Integer remainingCount
) {
    public static GameLeaveResponse from(GameLeaveResult result) {
        return new GameLeaveResponse(
                result.leftUserId(),
                result.remainingCount()
        );
    }
}
