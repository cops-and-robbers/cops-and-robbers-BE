package com.team.cops_and_robbers.game.game.application.dto.command;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.exception.GameException;

public record GameCreateCommand(
        Double playgroundLat,
        Double playgroundLng,
        Integer playgroundRadius,
        Double jailLat,
        Double jailLng,
        Integer jailRadius,

        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants
) {

    public void validate() {
        if (locationRevealIntervalMinutes >= roundDurationMinutes) {
            throw new ApplicationException(GameException.INVALID_LOCATION_INTERVAL);
        }
        if (policeWaitMinutes >= roundDurationMinutes) {
            throw new ApplicationException(GameException.INVALID_POLICE_WAIT_TIME);
        }
    }
}
