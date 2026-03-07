package com.team.cops_and_robbers.game.area.presentation.dto.response;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.game.area.application.dto.result.GameAreaResult;

public record GameAreaResponse(
        Coordinates playgroundCenter,
        Integer playgroundRadiusInMeters,
        Coordinates jailCenter,
        Integer jailRadiusInMeters
) {
    public static GameAreaResponse from(GameAreaResult result) {
        return new GameAreaResponse(
                result.playgroundCenter(),
                result.playgroundRadiusInMeters(),
                result.jailCenter(),
                result.jailRadiusInMeters()
        );
    }
}
