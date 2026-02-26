package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.team.cops_and_robbers.game.game.application.dto.result.GameInfoResult;

public record GameInfoResponse(
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants
) {
    public static GameInfoResponse from(GameInfoResult result) {
        return new GameInfoResponse(
                result.roundDurationMinutes(),
                result.locationRevealIntervalMinutes(),
                result.policeWaitMinutes(),
                result.maxParticipants()
        );
    }
}
