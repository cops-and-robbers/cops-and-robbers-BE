package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.team.cops_and_robbers.game.game.domain.Game;

public record GameCreateResponse(
        Long gameId,
        String inviteCode
) {

    public static GameCreateResponse from(Game game) {
        return new GameCreateResponse(
                game.getId(),
                game.getInviteCode()
        );
    }
}
