package com.team.cops_and_robbers.community.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;

public record CommunityChatRoomResult(
        Long postId,
        String title,
        RecruitmentStatus status,
        String meetingAt,
        Long memberCount,
        LastMessageResult lastMessage
) {
    public record LastMessageResult(
            Long id,
            String message,
            CommunityChatMessageType messageType,
            String createdAt
    ) {
        public static LastMessageResult from(CommunityChatMessage message) {
            return new LastMessageResult(
                    message.getId(),
                    message.getMessage(),
                    message.getMessageType(),
                    TimestampUtil.toIsoString(message.getCreatedAt())
            );
        }
    }

    /**
     * lastMessage는 아직 대화가 없는 방에서 null이 된다.
     */
    public static CommunityChatRoomResult of(CommunityPost post, long memberCount, CommunityChatMessage lastMessage) {
        return new CommunityChatRoomResult(
                post.getId(),
                post.getTitle(),
                post.getStatus(),
                TimestampUtil.toIsoString(post.getMeetingAt()),
                memberCount,
                lastMessage == null ? null : LastMessageResult.from(lastMessage)
        );
    }
}
