package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.game.area.domain.GameArea;

public record GameAreaUpdateResult(
        Double playgroundLatitude,
        Double playgroundLongitude,
        Integer playgroundRadiusInMeters,
        Double jailLatitude,
        Double jailLongitude,
        Integer jailRadiusInMeters
) {

    public static GameAreaUpdateResult from(GameArea gameArea) {
        return new GameAreaUpdateResult(
                gameArea.getPlaygroundCenter().getY(),
                gameArea.getPlaygroundCenter().getX(),
                gameArea.getPlaygroundRadiusInMeters(),
                gameArea.getJailCenter().getY(),
                gameArea.getJailCenter().getX(),
                gameArea.getJailRadiusInMeters()
        );
    }
}