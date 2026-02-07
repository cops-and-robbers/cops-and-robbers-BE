package com.team.cops_and_robbers.user.application.dto.command;

public record NicknameUpdateCommand(
        Long userId,
        String nickname
) {
    public static NicknameUpdateCommand of(Long userId, String nickname) {
        return new NicknameUpdateCommand(
                userId,
                nickname
        );
    }
}
