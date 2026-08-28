package com.team.cops_and_robbers.community.application.dto.result;

public record CommunityNotificationUnreadCountResult(
        long unreadCount
) {
    public static CommunityNotificationUnreadCountResult from(long unreadCount) {
        return new CommunityNotificationUnreadCountResult(unreadCount);
    }
}
