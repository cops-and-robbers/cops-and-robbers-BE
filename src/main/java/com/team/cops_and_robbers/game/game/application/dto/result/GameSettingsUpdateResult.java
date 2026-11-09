package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameMode;

public record GameSettingsUpdateResult(
        GameMode gameMode,
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants,
        Integer shrinkFinalRadiusInMeters
) {

    public static GameSettingsUpdateResult from(Game game) {
        return new GameSettingsUpdateResult(
                game.getGameMode(),
                game.getRoundDurationMinutes(),
                game.getLocationRevealIntervalMinutes(),
                game.getPoliceWaitMinutes(),
                game.getMaxParticipants(),
                game.getShrinkFinalRadiusInMeters()
        );
    }
}
