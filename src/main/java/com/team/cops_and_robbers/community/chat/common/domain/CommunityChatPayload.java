package com.team.cops_and_robbers.community.chat.common.domain;

import com.team.cops_and_robbers.common.util.TimestampUtil;

public record CommunityChatPayload(
        Long id,
        String messageKey,
        Long communityPostId,
        Long senderId,
        String senderNickname,
        int senderProfileIcon,
        String message,
        CommunityChatMessageType messageType,
        String createdAt
) {
    public static CommunityChatPayload from(CommunityChatMessage message) {
        return new CommunityChatPayload(
                message.getId(),
                message.getMessageKey(),
                message.getCommunityPostId(),
                message.getSenderId(),
                message.getSenderNickname(),
                message.getSenderProfileIcon(),
                message.getMessage(),
                message.getMessageType(),
                TimestampUtil.toIsoString(message.getCreatedAt())
        );
    }
}
