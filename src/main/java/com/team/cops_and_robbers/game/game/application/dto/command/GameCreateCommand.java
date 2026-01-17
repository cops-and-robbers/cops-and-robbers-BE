package com.team.cops_and_robbers.game.game.application.dto.command;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.presentation.dto.request.AreaRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameSettingsRequest;

public record GameCreateCommand(
        Long hostUserId,
        Double playgroundLatitude,
        Double playgroundLongitude,
        Integer playgroundRadiusInMeters,
        Double jailLatitude,
        Double jailLongitude,
        Integer jailRadiusInMeters,

        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants
) {

    public GameCreateCommand {
        if (locationRevealIntervalMinutes >= roundDurationMinutes) {
            throw new ApplicationException(GameException.INVALID_LOCATION_INTERVAL);
        }
        if (policeWaitMinutes >= roundDurationMinutes) {
            throw new ApplicationException(GameException.INVALID_POLICE_WAIT_TIME);
        }
    }

    public static GameCreateCommand of(Long hostUserId, AreaRequest area, GameSettingsRequest settings) {
        return new GameCreateCommand(
                hostUserId,
                area.playgroundCenter().latitude(),
                area.playgroundCenter().longitude(),
                area.playgroundRadiusInMeters(),
                area.jailCenter().latitude(),
                area.jailCenter().longitude(),
                area.jailRadiusInMeters(),
                settings.roundDurationMinutes(),
                settings.locationRevealIntervalMinutes(),
                settings.policeWaitMinutes(),
                settings.maxParticipants()
        );
    }
}
