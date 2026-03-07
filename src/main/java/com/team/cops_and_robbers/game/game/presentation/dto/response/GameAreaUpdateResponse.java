package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.game.game.application.dto.result.GameAreaUpdateResult;

public record GameAreaUpdateResponse(
        Coordinates playgroundCenter,
        Integer playgroundRadiusInMeters,
        Coordinates jailCenter,
        Integer jailRadiusInMeters
) {
    public static GameAreaUpdateResponse from(GameAreaUpdateResult result) {
        return new GameAreaUpdateResponse(
                result.playgroundCenter(),
                result.playgroundRadiusInMeters(),
                result.jailCenter(),
                result.jailRadiusInMeters()
        );
    }
}
