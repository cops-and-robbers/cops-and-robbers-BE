package com.team.cops_and_robbers.admin.application.dto.result.user;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.game.domain.GameMode;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;

import java.time.LocalDateTime;
import java.util.Objects;

public record UserGameHistoryResult(
        Long gameResultId,
        Long gameId,
        String endedAt,
        Team team,
        Team winnerTeam,
        GameEndReason endReason,
        Integer durationSeconds,
        Integer arrestCount,
        Integer arrestedCount,
        Boolean isMvp,
        Boolean isHost,
        GameMode gameMode,
        Boolean isEventGame,
        String leftAt
) {

    public static UserGameHistoryResult from(GameResultParticipant participant) {
        GameResult gameResult = participant.getGameResult();
        return new UserGameHistoryResult(
                gameResult.getId(),
                gameResult.getGameId(),
                toIsoOrNull(gameResult.getEndedAt()),
                participant.getTeam(),
                gameResult.getWinnerTeam(),
                gameResult.getEndReason(),
                gameResult.getDurationSeconds(),
                participant.getArrestCount(),
                participant.getArrestedCount(),
                participant.isMvp(),
                Objects.equals(participant.getUserId(), gameResult.getHostUserId()),
                gameResult.getGameMode(),
                Boolean.TRUE.equals(gameResult.getEventGame()),
                toIsoOrNull(participant.getLeftAt())
        );
    }

    private static String toIsoOrNull(LocalDateTime dateTime) {
        return dateTime == null ? null : TimestampUtil.toIsoString(dateTime);
    }
}
