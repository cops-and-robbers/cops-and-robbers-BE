package com.team.cops_and_robbers.user.application.dto.result;

import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;

public record UserGameInfoResult(
        Long gameId,
        Long participantId,
        GameStatus gameStatus,
        Team team
) {
    public static UserGameInfoResult from(GameParticipant participant) {
        return new UserGameInfoResult(
                participant.getGame().getId(),
                participant.getId(),
                participant.getGame().getStatus(),
                participant.getTeam()
        );
    }
}
