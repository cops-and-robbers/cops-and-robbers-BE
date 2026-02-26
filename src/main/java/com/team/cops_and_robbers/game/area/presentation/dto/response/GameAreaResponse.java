package com.team.cops_and_robbers.game.area.presentation.dto.response;

import com.team.cops_and_robbers.game.area.application.dto.result.GameAreaResult;

public record GameAreaResponse(
        CoordinatesResponse playgroundCenter,
        Integer playgroundRadiusInMeters,
        CoordinatesResponse jailCenter,
        Integer jailRadiusInMeters
) {
    public record CoordinatesResponse(
            Double latitude,
            Double longitude
    ) {
    }

    public static GameAreaResponse from(GameAreaResult result) {
        return new GameAreaResponse(
                new CoordinatesResponse(result.playgroundCenter().latitude(), result.playgroundCenter().longitude()),
                result.playgroundRadiusInMeters(),
                new CoordinatesResponse(result.jailCenter().latitude(), result.jailCenter().longitude()),
                result.jailRadiusInMeters()
        );
    }
}