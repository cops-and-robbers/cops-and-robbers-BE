package com.team.cops_and_robbers.community.presentation.dto.response;

import com.team.cops_and_robbers.community.application.dto.result.CommunityNotificationUnreadCountResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityNotificationUnreadCountResponse(
        @Schema(description = "안 읽은 알림 개수 (최근 60일 기준)", example = "3")
        long unreadCount
) {
    public static CommunityNotificationUnreadCountResponse from(CommunityNotificationUnreadCountResult result) {
        return new CommunityNotificationUnreadCountResponse(result.unreadCount());
    }
}
