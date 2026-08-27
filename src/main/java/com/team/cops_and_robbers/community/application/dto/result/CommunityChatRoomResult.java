package com.team.cops_and_robbers.community.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import com.team.cops_and_robbers.user.repository.UserProfileProjection;

public record CommunityChatRoomResult(
        Long postId,
        String title,
        RecruitmentStatus status,
        String meetingAt,
        Long memberCount,
        LastMessageResult lastMessage,
        long unreadCount,
        boolean notificationEnabled
) {
    public record LastMessageResult(
            Long id,
            String senderNickname,
            int senderProfileIcon,
            String message,
            CommunityChatMessageType messageType,
            String createdAt
    ) {
        /**
         * senderNickname·senderProfileIcon은 messages와 같은 규칙이다.
         * users의 현재 값을 우선하고, 탈퇴해 조회되지 않는 경우에만 발신 시점 값으로 대체한다.
         */
        public static LastMessageResult from(CommunityChatMessage message, UserProfileProjection currentProfile) {
            return new LastMessageResult(
                    message.getId(),
                    currentProfile != null ? currentProfile.nickname() : message.getSenderNickname(),
                    currentProfile != null ? currentProfile.profileIcon() : message.getSenderProfileIcon(),
                    message.getMessage(),
                    message.getMessageType(),
                    TimestampUtil.toIsoString(message.getCreatedAt())
            );
        }
    }

    /**
     * lastMessage는 아직 대화가 없는 방에서 null이 된다.
     */
    public static CommunityChatRoomResult of(
            CommunityPost post,
            long memberCount,
            CommunityChatMessage lastMessage,
            UserProfileProjection senderProfile,
            long unreadCount,
            boolean notificationEnabled
    ) {
        return new CommunityChatRoomResult(
                post.getId(),
                post.getTitle(),
                post.currentStatus(),
                TimestampUtil.toIsoString(post.getMeetingAt()),
                memberCount,
                lastMessage == null ? null : LastMessageResult.from(lastMessage, senderProfile),
                unreadCount,
                notificationEnabled
        );
    }
}
