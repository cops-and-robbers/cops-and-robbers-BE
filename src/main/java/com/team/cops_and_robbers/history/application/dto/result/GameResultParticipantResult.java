package com.team.cops_and_robbers.history.application.dto.result;

import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;

import java.time.LocalDateTime;

public record GameResultParticipantResult(
        String nickname,
        Team team,
        ParticipantStatus status,
        Integer arrestCount,
        LocalDateTime leftAt
) {
    public static GameResultParticipantResult from(GameResultParticipant participant) {
        return new GameResultParticipantResult(
                participant.getNickname(),
                participant.getTeam(),
                participant.getStatus(),
                participant.getArrestCount(),
                participant.getLeftAt()
        );
    }
}
