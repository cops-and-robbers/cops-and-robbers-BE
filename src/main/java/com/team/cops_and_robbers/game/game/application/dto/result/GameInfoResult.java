package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.game.domain.Game;

public record GameInfoResult(
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants,
        boolean isStarted,
        String gameStartTime
) {
    public static GameInfoResult from(Game game) {
        return new GameInfoResult(
                game.getRoundDurationMinutes(),
                game.getLocationRevealIntervalMinutes(),
                game.getPoliceWaitMinutes(),
                game.getMaxParticipants(),
                game.isInProgress(),
                game.getStartedAt() != null ? TimestampUtil.toIsoString(game.getStartedAt()) : null
        );
    }
}
