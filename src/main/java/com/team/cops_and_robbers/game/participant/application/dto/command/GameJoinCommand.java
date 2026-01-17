package com.team.cops_and_robbers.game.participant.application.dto.command;

public record GameJoinCommand(
        Long userId,
        Long gameId,
        String inviteCode
) {
    public static GameJoinCommand of(Long userId, Long gameId, String inviteCode) {
        return new GameJoinCommand(userId, gameId, inviteCode);
    }
}
