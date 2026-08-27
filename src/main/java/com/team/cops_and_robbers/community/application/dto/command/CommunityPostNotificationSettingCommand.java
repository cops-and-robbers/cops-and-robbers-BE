package com.team.cops_and_robbers.community.application.dto.command;

public record CommunityPostNotificationSettingCommand(
        Long userId,
        Long postId,
        boolean notifyComments,
        boolean notifyReplies
) {
    public static CommunityPostNotificationSettingCommand of(
            Long userId,
            Long postId,
            boolean notifyComments,
            boolean notifyReplies
    ) {
        return new CommunityPostNotificationSettingCommand(userId, postId, notifyComments, notifyReplies);
    }
}
