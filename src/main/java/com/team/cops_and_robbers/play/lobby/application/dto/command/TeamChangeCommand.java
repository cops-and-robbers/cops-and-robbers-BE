package com.team.cops_and_robbers.play.lobby.application.dto.command;

import com.team.cops_and_robbers.game.participant.domain.Team;

public record TeamChangeCommand(
        Long userId,
        Long gameId,
        Team targetTeam
) {
    public static TeamChangeCommand of(Long userId, Long gameId, Team targetTeam) {
        return new TeamChangeCommand(userId, gameId, targetTeam);
    }
}
