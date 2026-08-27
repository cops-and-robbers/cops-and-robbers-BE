package com.team.cops_and_robbers.community.application.dto.result;

import com.team.cops_and_robbers.community.domain.CommunityPostNotificationRole;
import com.team.cops_and_robbers.community.domain.CommunityPostNotificationSetting;

public record CommunityPostNotificationSettingResult(
        boolean notifyComments,
        boolean notifyReplies
) {
    public static CommunityPostNotificationSettingResult from(CommunityPostNotificationSetting setting) {
        return new CommunityPostNotificationSettingResult(
                setting.isNotifyComments(), setting.isNotifyReplies());
    }

    public static CommunityPostNotificationSettingResult from(CommunityPostNotificationRole role) {
        return new CommunityPostNotificationSettingResult(
                role.isNotifyComments(), role.isNotifyReplies());
    }
}
