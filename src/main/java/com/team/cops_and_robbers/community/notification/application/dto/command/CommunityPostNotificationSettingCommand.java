package com.team.cops_and_robbers.community.notification.application.dto.command;

public record CommunityPostNotificationSettingCommand(
        Long userId,
        Long postId,
        boolean commentNotificationsEnabled,
        boolean replyNotificationsEnabled
) {
    public static CommunityPostNotificationSettingCommand of(
            Long userId,
            Long postId,
            boolean commentNotificationsEnabled,
            boolean replyNotificationsEnabled
    ) {
        return new CommunityPostNotificationSettingCommand(userId, postId, commentNotificationsEnabled, replyNotificationsEnabled);
    }
}
