package com.team.cops_and_robbers.admin.application.dto.result.game;

import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;

public record AdminGameHistoryParticipantResult(
        Long userId,
        String nickname,
        Team team,
        ParticipantStatus status
) {

    public static AdminGameHistoryParticipantResult from(GameResultParticipant participant) {
        return new AdminGameHistoryParticipantResult(
                participant.getUserId(),
                participant.getNickname(),
                participant.getTeam(),
                participant.getStatus()
        );
    }
}
