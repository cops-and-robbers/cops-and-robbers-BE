package com.team.cops_and_robbers.game.area.application.dto.result;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.game.area.domain.GameArea;

public record GameAreaResult(
        Coordinates playgroundCenter,
        Integer playgroundRadiusInMeters,
        Coordinates jailCenter,
        Integer jailRadiusInMeters
) {
    public static GameAreaResult from(GameArea gameArea) {
        return new GameAreaResult(
                new Coordinates(gameArea.getPlaygroundCenter().getY(), gameArea.getPlaygroundCenter().getX()),
                gameArea.getPlaygroundRadiusInMeters(),
                new Coordinates(gameArea.getJailCenter().getY(), gameArea.getJailCenter().getX()),
                gameArea.getJailRadiusInMeters()
        );
    }
}
