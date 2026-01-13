package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.game.game.domain.Game;

public record GameCreateResult(
        Long gameId,
        String inviteCode
) {

    public static GameCreateResult from(Game game) {
        return new GameCreateResult(game.getId(), game.getInviteCode());
    }
}
