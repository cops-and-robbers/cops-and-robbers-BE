package com.team.cops_and_robbers.community.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.domain.CommunityNotification;
import com.team.cops_and_robbers.community.domain.CommunityNotificationType;

import java.time.LocalDateTime;

public record CommunityNotificationResult(
        Long id,
        CommunityNotificationType type,
        Long communityPostId,
        String postTitle,
        String content,
        boolean read,
        String createdAt
) {
    public static CommunityNotificationResult of(CommunityNotification notification, LocalDateTime readAt) {
        return new CommunityNotificationResult(
                notification.getId(),
                notification.getType(),
                notification.getCommunityPostId(),
                notification.getPostTitle(),
                notification.getContent(),
                notification.isRead(readAt),
                TimestampUtil.toIsoString(notification.getCreatedAt())
        );
    }
}
