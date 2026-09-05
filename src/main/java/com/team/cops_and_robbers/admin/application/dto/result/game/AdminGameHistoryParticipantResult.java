package com.team.cops_and_robbers.admin.application.dto.result.game;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;

import java.time.LocalDateTime;

public record AdminGameHistoryParticipantResult(
        Long userId,
        String nickname,
        Team team,
        ParticipantStatus status,
        String leftAt
) {

    public static AdminGameHistoryParticipantResult from(GameResultParticipant participant) {
        return new AdminGameHistoryParticipantResult(
                participant.getUserId(),
                participant.getNickname(),
                participant.getTeam(),
                participant.getStatus(),
                toIsoOrNull(participant.getLeftAt())
        );
    }

    private static String toIsoOrNull(LocalDateTime dateTime) {
        return dateTime == null ? null : TimestampUtil.toIsoString(dateTime);
    }
}
