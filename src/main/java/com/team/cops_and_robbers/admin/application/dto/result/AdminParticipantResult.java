package com.team.cops_and_robbers.admin.application.dto.result;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;

public record AdminParticipantResult(
        Long userId,
        String nickname,
        Team team,
        ParticipantStatus status,
        Boolean isHost
) {
    public static AdminParticipantResult from(GameParticipant participant) {
        return new AdminParticipantResult(
                participant.getUser().getId(),
                participant.getUser().getNickname(),
                participant.getTeam(),
                participant.getStatus(),
                participant.isHost()
        );
    }
}
