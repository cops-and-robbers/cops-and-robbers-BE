package com.team.cops_and_robbers.community.notification.application.dto.result;

import java.util.List;

public record CommunityNotificationListResult(
        List<CommunityNotificationResult> content,
        Long nextCursor,
        boolean hasNext
) {
}
