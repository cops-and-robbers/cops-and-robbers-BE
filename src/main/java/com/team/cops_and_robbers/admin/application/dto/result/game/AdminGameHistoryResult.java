package com.team.cops_and_robbers.admin.application.dto.result.game;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;

import java.time.LocalDateTime;

public record AdminGameHistoryResult(
        Long id,
        Long gameId,
        Integer roundNumber,
        Team winnerTeam,
        GameEndReason endReason,
        Integer totalPoliceCount,
        Integer totalRobberCount,
        Integer arrestedRobberCount,
        Integer totalArrestCount,
        Integer durationSeconds,
        String startedAt,
        String endedAt,
        Integer locationRevealIntervalMinutes,
        AreaType areaType,
        AdminGameAreaResult area,
        String createdAt
) {

    public static AdminGameHistoryResult from(GameResult gameResult) {
        return new AdminGameHistoryResult(
                gameResult.getId(),
                gameResult.getGameId(),
                gameResult.getRoundNumber(),
                gameResult.getWinnerTeam(),
                gameResult.getEndReason(),
                gameResult.getTotalPoliceCount(),
                gameResult.getTotalRobberCount(),
                gameResult.getArrestedRobberCount(),
                gameResult.getTotalArrestCount(),
                gameResult.getDurationSeconds(),
                toIsoOrNull(gameResult.getStartedAt()),
                toIsoOrNull(gameResult.getEndedAt()),
                gameResult.getLocationRevealIntervalMinutes(),
                gameResult.getAreaType(),
                toAreaResult(gameResult),
                TimestampUtil.toIsoString(gameResult.getCreatedAt())
        );
    }

    private static String toIsoOrNull(LocalDateTime dateTime) {
        return dateTime == null ? null : TimestampUtil.toIsoString(dateTime);
    }

    private static AdminGameAreaResult toAreaResult(GameResult gameResult) {
        if (gameResult.getPlaygroundCenter() == null && gameResult.getPlaygroundPolygon() == null) {
            return null;
        }
        return AdminGameAreaResult.from(gameResult);
    }
}
