package com.team.cops_and_robbers.admin.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;

public record AdminGameResult(
        Long id,
        String inviteCode,
        GameStatus status,
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants,
        Boolean isEventGame,
        String createdAt,
        String startedAt
) {
    public static AdminGameResult from(Game game) {
        String startedAt = game.getStartedAt() != null
                ? TimestampUtil.toIsoString(game.getStartedAt())
                : null;
        return new AdminGameResult(
                game.getId(),
                game.getInviteCode(),
                game.getStatus(),
                game.getRoundDurationMinutes(),
                game.getLocationRevealIntervalMinutes(),
                game.getPoliceWaitMinutes(),
                game.getMaxParticipants(),
                game.isEventGame(),
                TimestampUtil.toIsoString(game.getCreatedAt()),
                startedAt
        );
    }
}
