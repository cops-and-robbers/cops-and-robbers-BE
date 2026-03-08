package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.game.area.domain.GameArea;

public record GameAreaUpdateResult(
        Coordinates playgroundCenter,
        Integer playgroundRadiusInMeters,
        Coordinates jailCenter,
        Integer jailRadiusInMeters
) {
    public static GameAreaUpdateResult from(GameArea gameArea) {
        return new GameAreaUpdateResult(
                new Coordinates(gameArea.getPlaygroundCenter().getY(), gameArea.getPlaygroundCenter().getX()),
                gameArea.getPlaygroundRadiusInMeters(),
                new Coordinates(gameArea.getJailCenter().getY(), gameArea.getJailCenter().getX()),
                gameArea.getJailRadiusInMeters()
        );
    }
}
