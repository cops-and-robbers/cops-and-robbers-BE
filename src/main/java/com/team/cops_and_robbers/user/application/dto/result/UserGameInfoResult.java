package com.team.cops_and_robbers.user.application.dto.result;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;

public record UserGameInfoResult(
        Long gameId,
        Long participantId
) {
    public static UserGameInfoResult from(GameParticipant participant) {
        return new UserGameInfoResult(
                participant.getGame().getId(),
                participant.getId()
        );
    }
}
