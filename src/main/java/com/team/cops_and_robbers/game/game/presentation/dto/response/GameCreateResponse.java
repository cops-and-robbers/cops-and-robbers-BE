package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.team.cops_and_robbers.game.game.application.dto.result.GameCreateResult;

public record GameCreateResponse(
        Long gameId,
        String inviteCode
) {

    public static GameCreateResponse from(GameCreateResult result) {
        return new GameCreateResponse(
                result.gameId(),
                result.inviteCode()
        );
    }
}
