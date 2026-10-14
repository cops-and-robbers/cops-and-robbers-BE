package com.team.cops_and_robbers.admin.application.dto.command.game;

public record AdminUpdateEventGameCommand(
        Long gameId,
        boolean isEventGame
) {
}
