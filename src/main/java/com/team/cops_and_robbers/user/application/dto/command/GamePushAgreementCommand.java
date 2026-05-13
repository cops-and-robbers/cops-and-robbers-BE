package com.team.cops_and_robbers.user.application.dto.command;

public record GamePushAgreementCommand(
        Long userId,
        Boolean allowGamePush
) {
    public static GamePushAgreementCommand of(Long userId, Boolean allowGamePush) {
        return new GamePushAgreementCommand(userId, allowGamePush);
    }
}
