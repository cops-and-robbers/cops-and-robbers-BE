package com.team.cops_and_robbers.play.radio.application.dto.command;

public record RadioTokenCommand(Long gameId, Long userId) {

    public static RadioTokenCommand of(Long gameId, Long userId) {
        return new RadioTokenCommand(gameId, userId);
    }
}
