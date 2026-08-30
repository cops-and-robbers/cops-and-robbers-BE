package com.team.cops_and_robbers.community.chat.member.application.dto.command;

public record CommunityChatNotificationCommand(
        Long userId,
        Long postId,
        boolean allowNotification
) {
    public static CommunityChatNotificationCommand of(Long userId, Long postId, boolean allowNotification) {
        return new CommunityChatNotificationCommand(userId, postId, allowNotification);
    }
}
