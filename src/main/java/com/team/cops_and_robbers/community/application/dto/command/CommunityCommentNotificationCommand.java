package com.team.cops_and_robbers.community.application.dto.command;

public record CommunityCommentNotificationCommand(
        Long writerId,
        Long commentId,
        boolean replyNotificationsEnabled
) {
    public static CommunityCommentNotificationCommand of(Long writerId, Long commentId, boolean replyNotificationsEnabled) {
        return new CommunityCommentNotificationCommand(writerId, commentId, replyNotificationsEnabled);
    }
}
