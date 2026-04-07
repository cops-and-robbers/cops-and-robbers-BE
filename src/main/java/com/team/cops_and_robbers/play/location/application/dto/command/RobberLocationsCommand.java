package com.team.cops_and_robbers.play.location.application.dto.command;

public record RobberLocationsCommand(
        Long gameId,
        Long userId
) {
    public static RobberLocationsCommand of(Long gameId, Long userId) {
        return new RobberLocationsCommand(gameId, userId);
    }
}
