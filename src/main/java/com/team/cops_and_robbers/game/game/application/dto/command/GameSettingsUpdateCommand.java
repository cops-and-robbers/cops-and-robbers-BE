package com.team.cops_and_robbers.game.game.application.dto.command;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameSettingsRequest;

public record GameSettingsUpdateCommand(
        Long gameId,
        Long userId,
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer chaserWaitMinutes,
        Integer maxParticipants,
        Integer shrinkFinalRadiusInMeters
) {

    public GameSettingsUpdateCommand {
        if (locationRevealIntervalMinutes >= roundDurationMinutes) {
            throw new ApplicationException(GameException.INVALID_LOCATION_INTERVAL);
        }
        if (chaserWaitMinutes >= roundDurationMinutes) {
            throw new ApplicationException(GameException.INVALID_CHASER_WAIT_TIME);
        }
    }

    public static GameSettingsUpdateCommand of(Long gameId, Long userId, GameSettingsRequest settings) {
        return new GameSettingsUpdateCommand(
                gameId,
                userId,
                settings.roundDurationMinutes(),
                settings.locationRevealIntervalMinutes(),
                settings.chaserWaitMinutes(),
                settings.maxParticipants(),
                settings.shrinkFinalRadiusInMeters()
        );
    }
}
