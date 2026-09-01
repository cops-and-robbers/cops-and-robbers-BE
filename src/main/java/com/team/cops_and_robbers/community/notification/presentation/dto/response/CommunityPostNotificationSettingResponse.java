package com.team.cops_and_robbers.community.notification.presentation.dto.response;

import com.team.cops_and_robbers.community.notification.application.dto.result.CommunityPostNotificationSettingResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityPostNotificationSettingResponse(
        @Schema(description = "이 글에 달리는 댓글 알림 수신 여부", example = "true")
        boolean commentNotificationsEnabled,
        @Schema(description = "이 글에 달리는 답글 알림 수신 여부", example = "false")
        boolean replyNotificationsEnabled
) {
    public static CommunityPostNotificationSettingResponse from(CommunityPostNotificationSettingResult result) {
        if (result == null) {
            return null;
        }
        return new CommunityPostNotificationSettingResponse(result.commentNotificationsEnabled(), result.replyNotificationsEnabled());
    }
}
