package com.team.cops_and_robbers.game.participant.application.dto.command;

public record GameJoinCommand(
        Long userId,
        String inviteCode
) {
    public static GameJoinCommand of(Long userId, String inviteCode) {
        return new GameJoinCommand(userId, inviteCode);
    }
}
