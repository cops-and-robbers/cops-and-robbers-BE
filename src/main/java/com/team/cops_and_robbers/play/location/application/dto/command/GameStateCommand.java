package com.team.cops_and_robbers.play.location.application.dto.command;

public record GameStateCommand(Long gameId, Long userId) {

    public static GameStateCommand of(Long gameId, Long userId) {
        return new GameStateCommand(gameId, userId);
    }
}
