package com.team.cops_and_robbers.admin.application.dto.result.user;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;

public record GameParticipationResult(
        Long gameId,
        String inviteCode,
        Team team,
        ParticipantStatus status,
        Boolean isHost,
        String createdAt
) {
    public static GameParticipationResult from(GameParticipant participant) {
        return new GameParticipationResult(
                participant.getGame().getId(),
                participant.getGame().getInviteCode(),
                participant.getTeam(),
                participant.getStatus(),
                participant.isHost(),
                TimestampUtil.toIsoString(participant.getCreatedAt())
        );
    }
}
