package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.game.domain.Game;

public record GameCreateResult(
        Long gameId,
        String inviteCode,
        String status,
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants,
        String createdAt
) {

    public static GameCreateResult from(Game game) {
        return new GameCreateResult(
                game.getId(),
                game.getInviteCode(),
                game.getStatus().name(),
                game.getRoundDurationMinutes(),
                game.getLocationRevealIntervalMinutes(),
                game.getPoliceWaitMinutes(),
                game.getMaxParticipants(),
                TimestampUtil.toIsoString(game.getCreatedAt())
        );
    }
}
