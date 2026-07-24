package com.team.cops_and_robbers.admin.application.dto.result;

import com.team.cops_and_robbers.game.area.domain.GameArea;

public record AdminGameAreaResult(
        Double playgroundCenterLat,
        Double playgroundCenterLng,
        Integer playgroundRadiusInMeters,
        Double jailCenterLat,
        Double jailCenterLng,
        Integer jailRadiusInMeters
) {
    public static AdminGameAreaResult from(GameArea gameArea) {
        return new AdminGameAreaResult(
                gameArea.getPlaygroundCenter().getY(),
                gameArea.getPlaygroundCenter().getX(),
                gameArea.getPlaygroundRadiusInMeters(),
                gameArea.getJailCenter().getY(),
                gameArea.getJailCenter().getX(),
                gameArea.getJailRadiusInMeters()
        );
    }
}
