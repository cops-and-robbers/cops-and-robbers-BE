package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.game.game.domain.Game;

public record GameSettingsUpdateResult(
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants
) {

    public static GameSettingsUpdateResult from(Game game) {
        return new GameSettingsUpdateResult(
                game.getRoundDurationMinutes(),
                game.getLocationRevealIntervalMinutes(),
                game.getPoliceWaitMinutes(),
                game.getMaxParticipants()
        );
    }
}