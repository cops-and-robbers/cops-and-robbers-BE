package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.team.cops_and_robbers.game.game.application.dto.result.GameAreaUpdateResult;

public record GameAreaUpdateResponse(
        Double playgroundLatitude,
        Double playgroundLongitude,
        Integer playgroundRadiusInMeters,
        Double jailLatitude,
        Double jailLongitude,
        Integer jailRadiusInMeters
) {

    public static GameAreaUpdateResponse from(GameAreaUpdateResult result) {
        return new GameAreaUpdateResponse(
                result.playgroundLatitude(),
                result.playgroundLongitude(),
                result.playgroundRadiusInMeters(),
                result.jailLatitude(),
                result.jailLongitude(),
                result.jailRadiusInMeters()
        );
    }
}