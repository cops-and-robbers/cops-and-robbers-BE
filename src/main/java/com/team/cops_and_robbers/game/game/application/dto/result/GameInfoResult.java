package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.game.game.domain.Game;

import java.time.LocalDateTime;

public record GameInfoResult(
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants,
        LocalDateTime gameStartTime
) {
    public static GameInfoResult from(Game game) {
        return new GameInfoResult(
                game.getRoundDurationMinutes(),
                game.getLocationRevealIntervalMinutes(),
                game.getPoliceWaitMinutes(),
                game.getMaxParticipants(),
                game.getStartedAt()
        );
    }
}
