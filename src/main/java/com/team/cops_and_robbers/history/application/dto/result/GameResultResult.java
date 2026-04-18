package com.team.cops_and_robbers.history.application.dto.result;

import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameResult;

public record GameResultResult(
        Team winnerTeam,
        Integer durationSeconds,
        Integer arrestedRobberCount,
        Integer remainingRobberCount
) {
    public static GameResultResult from(GameResult gameResult) {
        int remaining = gameResult.getTotalRobberCount() - gameResult.getArrestedRobberCount();
        return new GameResultResult(
                gameResult.getWinnerTeam(),
                gameResult.getDurationSeconds(),
                gameResult.getArrestedRobberCount(),
                remaining
        );
    }
}
