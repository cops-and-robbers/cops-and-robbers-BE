package com.team.cops_and_robbers.game.area.application.dto.result;

import com.team.cops_and_robbers.game.area.domain.GameArea;

public record GameAreaResult(
        CoordinatesResult playgroundCenter,
        Integer playgroundRadiusInMeters,
        CoordinatesResult jailCenter,
        Integer jailRadiusInMeters
) {
    public record CoordinatesResult(
            Double latitude,
            Double longitude
    ) {
    }

    public static GameAreaResult from(GameArea gameArea) {
        return new GameAreaResult(
                new CoordinatesResult(gameArea.getPlaygroundCenter().getY(), gameArea.getPlaygroundCenter().getX()),
                gameArea.getPlaygroundRadiusInMeters(),
                new CoordinatesResult(gameArea.getJailCenter().getY(), gameArea.getJailCenter().getX()),
                gameArea.getJailRadiusInMeters()
        );
    }
}