package com.team.cops_and_robbers.play.system.application.dto;

public record EscapeCommand(
        Long gameId,
        Long escapedThiefUserId
) {
    public static EscapeCommand of(Long gameId, Long escapedThiefUserId) {
        return new EscapeCommand(gameId, escapedThiefUserId);
    }
}
