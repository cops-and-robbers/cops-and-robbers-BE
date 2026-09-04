package com.team.cops_and_robbers.admin.application.dto.result.game;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;

import java.time.LocalDateTime;

public record AdminGameDetailResult(
        Team winnerTeam,
        GameEndReason endReason,
        Integer totalPoliceCount,
        Integer totalRobberCount,
        Integer arrestedRobberCount,
        Integer durationSeconds,
        String startedAt,
        String endedAt,
        Integer locationRevealIntervalMinutes
) {
    public static AdminGameDetailResult from(GameResult gameResult) {
        return new AdminGameDetailResult(
                gameResult.getWinnerTeam(),
                gameResult.getEndReason(),
                gameResult.getTotalPoliceCount(),
                gameResult.getTotalRobberCount(),
                gameResult.getArrestedRobberCount(),
                gameResult.getDurationSeconds(),
                toIsoOrNull(gameResult.getStartedAt()),
                toIsoOrNull(gameResult.getEndedAt()),
                gameResult.getLocationRevealIntervalMinutes()
        );
    }

    private static String toIsoOrNull(LocalDateTime dateTime) {
        return dateTime == null ? null : TimestampUtil.toIsoString(dateTime);
    }
}
