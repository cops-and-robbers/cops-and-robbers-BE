package com.team.cops_and_robbers.admin.application.dto.result;

import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;

public record AdminGameResultDetail(
        Team winnerTeam,
        GameEndReason endReason,
        Integer totalPoliceCount,
        Integer totalRobberCount,
        Integer arrestedRobberCount,
        Integer durationSeconds,
        Double playgroundCenterLat,
        Double playgroundCenterLng,
        Integer playgroundRadiusInMeters
) {
    public static AdminGameResultDetail from(GameResult gameResult) {
        return new AdminGameResultDetail(
                gameResult.getWinnerTeam(),
                gameResult.getEndReason(),
                gameResult.getTotalPoliceCount(),
                gameResult.getTotalRobberCount(),
                gameResult.getArrestedRobberCount(),
                gameResult.getDurationSeconds(),
                gameResult.getPlaygroundCenter().getY(),
                gameResult.getPlaygroundCenter().getX(),
                gameResult.getPlaygroundRadiusInMeters()
        );
    }
}