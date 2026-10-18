package com.team.cops_and_robbers.history.application.dto.result;

import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;

import java.util.List;

public record GameResultResult(
        Team winnerTeam,
        Integer durationSeconds,
        Integer totalArrestCount,
        Integer remainingRobberCount,
        MvpInfo policeMvp,
        MvpInfo robberMvp
) {
    public static GameResultResult from(GameResult gameResult, List<GameResultParticipant> mvps) {
        int remaining = gameResult.getTotalRobberCount() - gameResult.getArrestedRobberCount();
        return new GameResultResult(
                gameResult.getWinnerTeam(),
                gameResult.getDurationSeconds(),
                gameResult.getTotalArrestCount(),
                remaining,
                findMvp(mvps, Team.POLICE),
                findMvp(mvps, Team.ROBBER)
        );
    }

    private static MvpInfo findMvp(List<GameResultParticipant> mvps, Team team) {
        return mvps.stream()
                .filter(participant -> participant.getTeam() == team)
                .findFirst()
                .map(MvpInfo::from)
                .orElse(null);
    }

    public record MvpInfo(String nickname) {
        public static MvpInfo from(GameResultParticipant participant) {
            return new MvpInfo(participant.getNickname());
        }
    }
}
