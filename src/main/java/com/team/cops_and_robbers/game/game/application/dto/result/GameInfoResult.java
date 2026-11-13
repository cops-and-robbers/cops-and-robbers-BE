package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameMode;

import java.time.LocalDateTime;

public record GameInfoResult(
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants,
        Integer shrinkFinalRadiusInMeters,
        boolean isStarted,
        LocalDateTime gameStartTime,
        boolean isEventGame,
        GameMode gameMode
) {
    public static GameInfoResult from(Game game) {
        return new GameInfoResult(
                game.getRoundDurationMinutes(),
                game.getLocationRevealIntervalMinutes(),
                game.getPoliceWaitMinutes(),
                game.getMaxParticipants(),
                game.getShrinkFinalRadiusInMeters(),
                game.isInProgress(),
                game.getStartedAt(),
                game.isEventGame(),
                game.getGameMode()
        );
    }
}
