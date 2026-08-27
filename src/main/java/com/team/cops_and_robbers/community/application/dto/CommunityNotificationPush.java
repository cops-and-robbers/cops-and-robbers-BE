package com.team.cops_and_robbers.community.application.dto;

import com.team.cops_and_robbers.community.domain.CommunityComment;
import com.team.cops_and_robbers.community.domain.CommunityNotificationType;
import com.team.cops_and_robbers.community.domain.CommunityPost;

public record CommunityNotificationPush(
        CommunityNotificationType type,
        Long communityPostId,
        String postTitle,
        String content
) {
    public static CommunityNotificationPush of(
            CommunityNotificationType type,
            CommunityPost post,
            CommunityComment comment
    ) {
        return new CommunityNotificationPush(type, post.getId(), post.getTitle(), comment.getContent());
    }
}
