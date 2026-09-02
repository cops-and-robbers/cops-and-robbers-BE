package com.team.cops_and_robbers.community.notification.application.dto;

import java.util.List;

public record CommunityNotificationDispatch(
        List<Long> recipients,
        CommunityNotificationPush push
) {
    public static CommunityNotificationDispatch none() {
        return new CommunityNotificationDispatch(List.of(), null);
    }

    public boolean isEmpty() {
        return recipients.isEmpty();
    }
}
