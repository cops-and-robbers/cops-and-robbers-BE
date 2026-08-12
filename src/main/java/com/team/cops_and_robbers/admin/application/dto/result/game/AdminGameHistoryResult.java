package com.team.cops_and_robbers.admin.application.dto.result.game;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;

public record AdminGameHistoryResult(
        Long id,
        Long gameId,
        Team winnerTeam,
        GameEndReason endReason,
        Integer totalPoliceCount,
        Integer totalRobberCount,
        Integer arrestedRobberCount,
        Integer totalArrestCount,
        Integer durationSeconds,
        AreaType areaType,
        String createdAt
) {

    public static AdminGameHistoryResult from(GameResult gameResult) {
        return new AdminGameHistoryResult(
                gameResult.getId(),
                gameResult.getGameId(),
                gameResult.getWinnerTeam(),
                gameResult.getEndReason(),
                gameResult.getTotalPoliceCount(),
                gameResult.getTotalRobberCount(),
                gameResult.getArrestedRobberCount(),
                gameResult.getTotalArrestCount(),
                gameResult.getDurationSeconds(),
                gameResult.getAreaType(),
                TimestampUtil.toIsoString(gameResult.getCreatedAt())
        );
    }
}
