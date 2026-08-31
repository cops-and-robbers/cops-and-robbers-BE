package com.team.cops_and_robbers.user.application.dto.command;

public record ProfileIconUpdateCommand(
        Long userId,
        int profileIcon
) {
    public static ProfileIconUpdateCommand of(Long userId, int profileIcon) {
        return new ProfileIconUpdateCommand(
                userId,
                profileIcon
        );
    }
}
