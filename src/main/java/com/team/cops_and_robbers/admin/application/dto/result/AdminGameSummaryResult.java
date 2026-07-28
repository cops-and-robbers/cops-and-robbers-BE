package com.team.cops_and_robbers.admin.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;

public record AdminGameSummaryResult(
        Long id,
        String inviteCode,
        GameStatus status,
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer maxParticipants,
        Boolean isEventGame,
        Integer participantCount,
        String createdAt
) {
    public static AdminGameSummaryResult from(Game game, int participantCount) {
        return new AdminGameSummaryResult(
                game.getId(),
                game.getInviteCode(),
                game.getStatus(),
                game.getRoundDurationMinutes(),
                game.getLocationRevealIntervalMinutes(),
                game.getMaxParticipants(),
                game.isEventGame(),
                participantCount,
                TimestampUtil.toIsoString(game.getCreatedAt())
        );
    }
}
