package com.team.cops_and_robbers.community.application.dto;

import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.user.repository.UserProfileProjection;

import java.util.Map;

public record CommunityChatRoomListContext(
        Map<Long, Long> memberCounts,
        Map<Long, CommunityChatMessage> lastMessages,
        Map<Long, UserProfileProjection> senderProfiles,
        Map<Long, Long> unreadCounts
) {
    public static CommunityChatRoomListContext of(
            Map<Long, Long> memberCounts,
            Map<Long, CommunityChatMessage> lastMessages,
            Map<Long, UserProfileProjection> senderProfiles,
            Map<Long, Long> unreadCounts
    ) {
        return new CommunityChatRoomListContext(
                memberCounts, lastMessages, senderProfiles, unreadCounts);
    }
}
