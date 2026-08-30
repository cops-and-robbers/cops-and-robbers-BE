package com.team.cops_and_robbers.community.notification.application.dto.result;

import com.team.cops_and_robbers.community.notification.domain.CommunityPostNotificationRole;
import com.team.cops_and_robbers.community.notification.domain.CommunityPostNotificationSetting;

public record CommunityPostNotificationSettingResult(
        boolean commentNotificationsEnabled,
        boolean replyNotificationsEnabled
) {
    public static CommunityPostNotificationSettingResult from(CommunityPostNotificationSetting setting) {
        return new CommunityPostNotificationSettingResult(
                setting.isCommentNotificationsEnabled(), setting.isReplyNotificationsEnabled());
    }

    public static CommunityPostNotificationSettingResult from(CommunityPostNotificationRole role) {
        return new CommunityPostNotificationSettingResult(
                role.isCommentNotificationsEnabled(), role.isReplyNotificationsEnabled());
    }
}
