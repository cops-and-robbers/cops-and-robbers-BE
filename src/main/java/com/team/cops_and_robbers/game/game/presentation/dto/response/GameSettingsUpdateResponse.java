package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.team.cops_and_robbers.game.game.application.dto.result.GameSettingsUpdateResult;

public record GameSettingsUpdateResponse(
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants
) {

    public static GameSettingsUpdateResponse from(GameSettingsUpdateResult result) {
        return new GameSettingsUpdateResponse(
                result.roundDurationMinutes(),
                result.locationRevealIntervalMinutes(),
                result.policeWaitMinutes(),
                result.maxParticipants()
        );
    }
}
