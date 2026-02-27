package com.team.cops_and_robbers.game.game.application.dto.command;

import com.team.cops_and_robbers.game.game.presentation.dto.request.GameAreaRequest;

public record GameAreaUpdateCommand(
        Long gameId,
        Long userId,
        Double playgroundLatitude,
        Double playgroundLongitude,
        Integer playgroundRadiusInMeters,
        Double jailLatitude,
        Double jailLongitude,
        Integer jailRadiusInMeters
) {

    public static GameAreaUpdateCommand of(Long gameId, Long userId, GameAreaRequest area) {
        return new GameAreaUpdateCommand(
                gameId,
                userId,
                area.playgroundCenter().latitude(),
                area.playgroundCenter().longitude(),
                area.playgroundRadiusInMeters(),
                area.jailCenter().latitude(),
                area.jailCenter().longitude(),
                area.jailRadiusInMeters()
        );
    }
}
