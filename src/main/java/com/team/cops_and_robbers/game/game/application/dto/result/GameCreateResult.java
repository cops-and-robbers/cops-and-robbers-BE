package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameMode;

public record GameCreateResult(
        Long gameId,
        String inviteCode,
        String status,
        GameMode gameMode,
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants,
        Integer shrinkFinalRadiusInMeters,
        String createdAt
) {

    public static GameCreateResult from(Game game) {
        return new GameCreateResult(
                game.getId(),
                game.getInviteCode(),
                game.getStatus().name(),
                game.getGameMode(),
                game.getRoundDurationMinutes(),
                game.getLocationRevealIntervalMinutes(),
                game.getPoliceWaitMinutes(),
                game.getMaxParticipants(),
                game.getShrinkFinalRadiusInMeters(),
                TimestampUtil.toIsoString(game.getCreatedAt())
        );
    }
}
